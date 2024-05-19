package git4s.data.parser

import cats.effect.kernel.Async
import fs2.io.file.Path
import fs2.{Chunk, Pipe, Pull}
import git4s.data.diff.FileDiff.*
import git4s.data.diff.{CodeBlock, FileDiff}
import git4s.utils.*

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

  def apply[F[_]](using p: FileDiffParser[F]): FileDiffParser[F] = p

  given [F[_]](using F: Async[F]): FileDiffParser[F] with
    def parse: Pipe[F, String, FileDiff] = (originalStream: fs2.Stream[F, String]) =>

      // ------------------ EDITED FILE ------------------
      def goChangedFile[D <: FileDiff](stream: fs2.Stream[F, String])(
        f: ((Path, Path), (CodeBlock, CodeBlock)) => D
      ): Pull[F, D, fs2.Stream[F, String]] = {

        def goSingleCodeBlock(
          s: fs2.Stream[F, String]
        )(symbol: Char, linesInfoStr: String): Pull[F, Nothing, (CodeBlock, fs2.Stream[F, String])] =
          s.pull
            .unconsUntil(_.startsWith(symbol.toString))
            .flatMap { case (lines, rms) =>
              val linesInfo = linesInfoStr match
                case s"$s,$n" => (s.toIntOption, n.toIntOption)
                case s"$s"    => (None, s.toIntOption)

              Pull.pure(
                CodeBlock(
                  startLine  = linesInfo._1.getOrElse(0),
                  linesCount = linesInfo._2.getOrElse(0),
                  lines      = lines.map(_.drop(1)) // remove the symbol
                ) -> rms
              )
            }

        def goChangeBlock(s: fs2.Stream[F, String]): Pull[F, Nothing, ((CodeBlock, CodeBlock), fs2.Stream[F, String])] =
          s.pull.uncons1.flatMap {
            case Some((s"@@ -$beforeLinesStr +$afterLinesStr @@", rms1)) =>
              goSingleCodeBlock(rms1)('-', beforeLinesStr).flatMap { case (beforeCodeblock, rms2) =>
                goSingleCodeBlock(rms2)('+', afterLinesStr).map { case (afterCodeblock, rms3) =>
                  ((beforeCodeblock, afterCodeblock), rms3)
                }
              }
            case Some((x, _)) =>
              Pull.raiseError(MalformedDiff(x))
            case None =>
              Pull.raiseError(PrematureEOS)
          }

        stream.drop(1).pull.unconsN(2).flatMap {
          case Some(chunk, rms2: fs2.Stream[F, String]) =>
            chunk.toList match
              case s"$idA $_/$pathA" :: s"$idB $_/$pathB" :: Nil =>
                goChangeBlock(rms2).flatMap { case (changes, rms3) =>
                  Pull
                    .output1(f((Path(pathA), Path(pathB)), changes))
                    .as(rms3)
                }
              case x =>
                Pull.raiseError(MalformedDiff(x.mkString("\n")))
          case _ =>
            Pull.raiseError(PrematureEOS)
        }
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
            goChangedFile(rms)((p, b) => NewFile(p._2, b._2)).flatMap(go(_))

          case Some(s"deleted file mode $_", rms: fs2.Stream[F, String]) =>
            goChangedFile(rms)((p, b) => DeletedFile(p._1, b._1)).flatMap(go(_))

          case Some(s"rename from $from", rms: fs2.Stream[F, String]) =>
            goMovedOrRenamedFile(from, rms).flatMap(go(_))

//          case Some(s"index $_", rms: fs2.Stream[F, String]) =>
//            goChangedFile(ChangeType.DeletedLine)(rms)
//              .flatMap { case (newFile, rms2) => Pull.output1(newFile) >> go(rms2) }

          // skip line
          case Some(_, rms) =>
            go(rms)
          case _ =>
            // no more elements
            Pull.done
        }

      go(originalStream).stream
