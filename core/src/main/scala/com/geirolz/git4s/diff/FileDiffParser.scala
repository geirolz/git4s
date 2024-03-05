package com.geirolz.git4s.diff

import cats.effect.kernel.Async
import com.geirolz.git4s.data.diff.{CodeBlock, FileDiff}
import com.geirolz.git4s.data.diff.FileDiff.*
import fs2.io.file.Path
import fs2.{Pipe, Pull}

import scala.util.control.NoStackTrace

trait FileDiffParser[F[_]]:
  def parse: Pipe[F, String, FileDiff]

object FileDiffParser:

  sealed trait ParsingDiffFailure extends NoStackTrace:
    override def getMessage: String = this match
      case PrematureEOS          => "Premature end of stream."
      case MalformedDiff(actual) => s"Malformed diff. Not expected $actual."

  case object PrematureEOS extends ParsingDiffFailure
  case class MalformedDiff(actual: String) extends ParsingDiffFailure

  def apply[F[_]](using p: FileDiffParser[F]): FileDiffParser[F] = p

  given [F[_]](using F: Async[F]): FileDiffParser[F] =
    new FileDiffParser[F]:
      def parse: Pipe[F, String, FileDiff] = (s: fs2.Stream[F, String]) =>

        // ------------------ NEW OR DELETED FILE ------------------
        def goNewOrDeletedFile(newFile: Boolean)(
          stream: fs2.Stream[F, String]
        ): Pull[F, Nothing, (NewFile | DeletedFile, fs2.Stream[F, String])] =

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

                      Pull.pure((fileDiff, rms2.drop(lines.size)))
                    case None =>
                      // empty file - stream ended
                      Pull.pure(f(Path(path), CodeBlock.empty), fs2.Stream.empty)
                  }
                case x =>
                  Pull.raiseError(MalformedDiff(x.mkString("\n")))
              }
            case _ =>
              Pull.raiseError(PrematureEOS)
          }

        // ------------------ NEW OR DELETED FILE ------------------
        def goMovedOrRenamedFile(
          from: String,
          stream: fs2.Stream[F, String]
        ): Pull[F, Nothing, (RenamedFile, fs2.Stream[F, String])] =
          stream.pull.uncons1.flatMap {
            case Some((s"rename to $to", rms2)) =>
              Pull.pure((RenamedFile(Path(from), Path(to)), rms2))
            case Some((x, _)) =>
              Pull.raiseError(MalformedDiff(x))
            case None =>
              Pull.raiseError(PrematureEOS)
          }

        def go(s: fs2.Stream[F, String]): Pull[F, FileDiff, Unit] =
          s.pull.uncons1.flatMap {
            case Some(s"new file mode $_", rms: fs2.Stream[F, String]) =>
              goNewOrDeletedFile(newFile = true)(rms).flatMap { case (newFile, rms2) =>
                Pull.output1(newFile) >> go(rms2)
              }

            case Some(s"deleted file mode $_", rms: fs2.Stream[F, String]) =>
              goNewOrDeletedFile(newFile = false)(rms)
                .flatMap { case (newFile, rms2) => Pull.output1(newFile) >> go(rms2) }

            case Some(s"rename from $from", rms: fs2.Stream[F, String]) =>
              goMovedOrRenamedFile(from, rms)
                .flatMap { case (renamedFile, rms2) => Pull.output1(renamedFile) >> go(rms2) }

            // skip line
            case Some(_, rms) =>
              go(rms)
            case _ =>
              // no more elements
              Pull.done
          }

        go(s).stream
