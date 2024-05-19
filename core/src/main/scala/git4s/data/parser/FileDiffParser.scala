package git4s.data.parser

import cats.effect.kernel.Async
import git4s.data.diff.FileDiff.*
import fs2.io.file.Path
import fs2.{Pipe, Pull}
import git4s.data.diff.{CodeBlock, FileDiff}

import scala.util.control.NoStackTrace

// TODO WIP
private[git4s] trait FileDiffParser[F[_]]:
  def parse: Pipe[F, String, FileDiff]

// TODO WIP
private[git4s] object FileDiffParser:

  // errors
  sealed trait ParsingDiffFailure extends NoStackTrace:
    override def getMessage: String = this match
      case PrematureEOS          => "Premature end of stream."
      case MalformedDiff(actual) => s"Malformed diff. Not expected $actual."
  case object PrematureEOS extends ParsingDiffFailure
  case class MalformedDiff(actual: String) extends ParsingDiffFailure

  // changes types
  private sealed trait ChangeType(val symbol: String)
  private object ChangeType:
    case object NewLines extends ChangeType("+")
    case object DeletedLines extends ChangeType("-")

  def apply[F[_]](using p: FileDiffParser[F]): FileDiffParser[F] = p

  given [F[_]](using F: Async[F]): FileDiffParser[F] with
    def parse: Pipe[F, String, FileDiff] = (s: fs2.Stream[F, String]) =>

      // ------------------ EDITED FILE ------------------
      def goChangeBlock(
        tpe: ChangeType,
        source: fs2.Stream[F, String]
      ): Pull[F, Nothing, (CodeBlock, fs2.Stream[F, String])] =
        source.groupAdjacentBy(_.startsWith(tpe.symbol)).pull.uncons1.flatMap {
          case Some(((_, lines), rms)) =>
            val codeBlock = CodeBlock.withoutInfo(lines.map(_.drop(1)))
            Pull.pure((codeBlock, source.drop(lines.size)))
          case None =>
            // empty file - stream ended
            Pull.pure((CodeBlock.empty, fs2.Stream.empty))
        }

      /*
        diff --git a/baz.md b/baz.md
        new file mode 100644
        index 0000000..aa39060
        --- /dev/null
        +++ b/newfile.md
        @@ -0,0 +1 @@
        +newfile
       * */
      // ------------------ NEW OR DELETED FILE ------------------
      def goNewOrDeletedFile(tpe: ChangeType)(
        stream: fs2.Stream[F, String]
      ): Pull[F, NewFile | DeletedFile, fs2.Stream[F, String]] =

        val isNewFile: Boolean = tpe == ChangeType.NewLines
        val f: (Path, CodeBlock) => NewFile | DeletedFile = tpe match
          case ChangeType.NewLines     => NewFile(_, _)
          case ChangeType.DeletedLines => DeletedFile(_, _)

        stream.drop(1).pull.unconsN(3).flatMap {
          case Some(chunk, rms2: fs2.Stream[F, String]) =>
            chunk.toList match {
              case s"$idA $_/$pathA" :: s"$idB $_/$pathB" :: s"@@ -$scol,$sline +$ecol,$eline @@" :: Nil =>
                val path = if (isNewFile) pathA else pathB
                goChangeBlock(tpe, rms2).flatMap { case (changes, rms3) =>
                  val fileDiff: NewFile | DeletedFile =
                    f(
                      Path(path),
                      changes.copy(
                        startLine   = sline.toInt,
                        startColumn = scol.toInt,
                        endLine     = eline.toInt,
                        endColumn   = ecol.toInt
                      )
                    )

                  Pull.output1(fileDiff).as(rms3)
                }
              case x =>
                Pull.raiseError(MalformedDiff(x.mkString("\n")))
            }
          case _ =>
            Pull.raiseError(PrematureEOS)
        }

      // ------------------ MOVED OR RENAMED FILE ------------------
      def goMovedOrRenamedFile(
        from: String,
        stream: fs2.Stream[F, String]
      ): Pull[F, RenamedFile, fs2.Stream[F, String]] =
        stream.pull.uncons1.flatMap {
          case Some((s"rename to $to", rms2)) =>
            Pull.output1(RenamedFile(Path(from), Path(to))).as(rms2)
          case Some((x, _)) =>
            Pull.raiseError(MalformedDiff(x))
          case None =>
            Pull.raiseError(PrematureEOS)
        }

      def go(s: fs2.Stream[F, String]): Pull[F, FileDiff, Unit] =
        s.pull.uncons1.flatMap {

          case Some(s"new file mode $_", rms: fs2.Stream[F, String]) =>
            goNewOrDeletedFile(ChangeType.NewLines)(rms).flatMap(go(_))

          case Some(s"deleted file mode $_", rms: fs2.Stream[F, String]) =>
            goNewOrDeletedFile(ChangeType.DeletedLines)(rms).flatMap(go(_))

          case Some(s"rename from $from", rms: fs2.Stream[F, String]) =>
            goMovedOrRenamedFile(from, rms).flatMap(go(_))

//          case Some(s"index $_", rms: fs2.Stream[F, String]) =>
//            goNewOrDeletedFile(ChangeType.DeletedLine)(rms)
//              .flatMap { case (newFile, rms2) => Pull.output1(newFile) >> go(rms2) }

          // skip line
          case Some(_, rms) =>
            go(rms)
          case _ =>
            // no more elements
            Pull.done
        }

      go(s).stream
