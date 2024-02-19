package com.geirolz.git4s.data.diff

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder.Result
import com.geirolz.git4s.codec.{CmdDecoder, DecodingFailure}
import com.geirolz.git4s.data.diff.FileDiff.{DeletedFile, ModifiedFile, NewFile, RenamedFile}
import fs2.io.file.Path
import fs2.{Chunk, Pull}

sealed trait FileDiff(
  sourceFile: Option[Path],
  targetFile: Option[Path],
  changes: LazyList[(CodeBlock, CodeBlock)]
):
  def isNewFile: Boolean      = this.isInstanceOf[FileDiff.NewFile]
  def isDeletedFile: Boolean  = this.isInstanceOf[FileDiff.DeletedFile]
  def isModifiedFile: Boolean = this.isInstanceOf[FileDiff.ModifiedFile]
  def isRenamedFile: Boolean  = this.isInstanceOf[FileDiff.RenamedFile]

object FileDiff extends FileDiffInstances:

  case class NewFile(
    newFile: Path,
    content: CodeBlock
  ) extends FileDiff(None, Some(newFile), LazyList((CodeBlock.empty, content)))
  object NewFile:
    def empty(path: Path): NewFile = NewFile(path, CodeBlock.empty)

  case class DeletedFile(
    sourceFile: Path,
    content: CodeBlock
  ) extends FileDiff(Some(sourceFile), None, LazyList((content, CodeBlock.empty)))

  case class ModifiedFile(
    sourceFile: Path,
    newFile: Path,
    changes: LazyList[(CodeBlock, CodeBlock)]
  ) extends FileDiff(Some(sourceFile), Some(newFile), changes):

    def addChanges(changes: (CodeBlock, CodeBlock)): ModifiedFile =
      this.copy(changes = changes #:: this.changes)

  case class RenamedFile(
    sourceFile: Path,
    newFile: Path
  ) extends FileDiff(Some(sourceFile), Some(newFile), LazyList.empty)

sealed trait FileDiffInstances:

  given [F[_]: Async]: CmdDecoder[F, FileDiff] =
    CmdDecoder.lines { (s: fs2.Stream[F, String]) =>

      val prematureEnd         = Left(DecodingFailure(s"Premature end of stream."))
      def malformed(x: String) = Left(DecodingFailure(s"Malformed file output. Not expected $x"))

      // ------------------ NEW OR DELETED FILE ------------------
      def goNewOrDeletedFile(newFile: Boolean)(
        stream: fs2.Stream[F, String]
      ): Pull[F, Nothing, CmdDecoder.Result[(NewFile | DeletedFile, fs2.Stream[F, String])]] =

        val singleId = if newFile then "+" else "-"
        val f        = if newFile then NewFile(_, _) else DeletedFile(_, _)

        stream.drop(1).pull.unconsN(3).flatMap {
          case Some(chunk, rms2: fs2.Stream[F, String]) =>
            chunk.toList match {
              case s"$idA $_/$pathA" :: s"$idB $_/$pathB" :: s"@@ -$scol,$sline +$ecol,$eline @@" :: Nil =>
                val path = if (newFile) pathA else pathB
                rms2.groupAdjacentBy(_.startsWith(singleId)).pull.uncons1.flatMap {
                  case Some(((_, lines), rms3)) =>
                    val fileDiff: NewFile | DeletedFile =
                      f(
                        Path(path),
                        CodeBlock(
                          startLine   = sline.toInt,
                          startColumn = scol.toInt,
                          endLine     = eline.toInt,
                          endColumn   = ecol.toInt,
                          lines       = lines.map(_.drop(1))
                        )
                      )

                    Pull.pure(Right((fileDiff, rms2.drop(lines.size))))
                  case None =>
                    // empty file - stream ended
                    Pull.pure(Right(f(Path(path), CodeBlock.empty), fs2.Stream.empty))
                }
              case x =>
                Pull.pure(malformed(x.mkString("\n")))
            }
          case _ =>
            Pull.pure(prematureEnd)
        }

      // ------------------ NEW OR DELETED FILE ------------------
      def goMovedOrRenamedFile(
        from: String,
        stream: fs2.Stream[F, String]
      ): Pull[F, Nothing, Either[DecodingFailure, RenamedFile]] =
        stream.pull.uncons1.flatMap {
          case Some((s"rename to $to", rms2)) =>
            Pull.pure(Right(RenamedFile(Path(from), Path(to))))
          case Some((x, _)) =>
            Pull.pure(malformed(x))
          case None =>
            Pull.pure(prematureEnd)
        }

      def go(s: fs2.Stream[F, String]): Pull[F, CmdDecoder.Result[FileDiff], Unit] =
        s.pull.uncons1.flatMap {
          case Some(s"new file mode $_", rms: fs2.Stream[F, String]) =>
            goNewOrDeletedFile(newFile = true)(rms).flatMap {
              case Right((newFile, rms2)) =>
                Pull.output1(Right(newFile)) >> go(rms2)
              case Left(err) =>
                Pull.output1(Left(err))
            }

          case Some(s"deleted file mode $_", rms: fs2.Stream[F, String]) =>
            goNewOrDeletedFile(newFile = false)(rms).flatMap {
              case Right((newFile, rms2)) =>
                Pull.output1(Right(newFile)) >> go(rms2)
              case Left(err) =>
                Pull.output1(Left(err))
            }

          case Some(s"rename from $from", rms: fs2.Stream[F, String]) =>
            goMovedOrRenamedFile(from, rms).flatMap {
              case Right(renamedFile) =>
                Pull.output1(Right(renamedFile)) >> go(rms)
              case Left(err) =>
                Pull.output1(Left(err))
            }

          // skip line
          case Some(_, rms) =>
            go(rms)
          case _ =>
            // no more elements
            Pull.done
        }

      go(s).stream
    }
