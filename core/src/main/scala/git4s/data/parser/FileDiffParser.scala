import fs2.{Chunk, Pipe, Pull}
import git4s.data.diff.FileDiff.*
import git4s.utils.*
    def parse: Pipe[F, String, FileDiff] = (originalStream: fs2.Stream[F, String]) =>
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
            chunk.toList match
              case s"$idA $_/$pathA" :: s"$idB $_/$pathB" :: Nil =>
                goChangeBlock(rms2).flatMap { case (changes, rms3) =>
                  Pull
                    .output1(f((Path(pathA), Path(pathB)), changes))
                    .as(rms3)
      }
      ): Pull[F, RenamedFile, fs2.Stream[F, String]] =
            Pull.output1(RenamedFile(Path(from), Path(to))).as(rms2)

            goChangedFile(rms)((p, b) => NewFile(p._2, b._2)).flatMap(go(_))
            goChangedFile(rms)((p, b) => DeletedFile(p._1, b._1)).flatMap(go(_))
            goMovedOrRenamedFile(from, rms).flatMap(go(_))
//            goChangedFile(ChangeType.DeletedLine)(rms)
      go(originalStream).stream