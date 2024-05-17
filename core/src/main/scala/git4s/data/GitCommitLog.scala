package git4s.data

import cats.effect.kernel.Async
import fs2.{Chunk, Pull}
import git4s.utils.*
import cats.syntax.all.*
import git4s.codec.{CmdDecoder, DecodingFailure}
import git4s.data.value.{Author, CommitDate, CommitId, CommitMessage}

case class GitCommitLog(
  commitId: CommitId,
  merge: Option[(CommitId, CommitId)],
  author: Author,
  date: CommitDate,
  message: CommitMessage
)
object GitCommitLog:

  given [F[_]: Async]: CmdDecoder[F, GitCommitLog] =
    CmdDecoder.lines { lines =>

      def go(lines: fs2.Stream[F, String]): Pull[F, CmdDecoder.Result[GitCommitLog], Option[fs2.Stream[F, String]]] =
        lines.pull.uncons1.flatMap {
          case Some((s"commit $commitId", rms)) =>
            rms.pull.unconsUnless(_.startsWith("commit ")).flatMap {
              case Some((chunk, rms)) =>
                chunk.toList match
                  case s"Author: $author" :: s"Date: $date" :: message =>
                    Pull.output1(
                      GitCommitLog(
                        commitId = CommitId(commitId),
                        merge    = None,
                        author   = Author(author),
                        date     = CommitDate(date.trim),
                        message  = CommitMessage(message.mkString.trim)
                      ).asRight
                    ) >> go(rms)
                  case s"Merge: $c1 $c2" :: s"Author: $author" :: s"Date: $date" :: message =>
                    Pull.output1(
                      GitCommitLog(
                        commitId = CommitId(commitId),
                        merge    = Some((CommitId(c1), CommitId(c2))),
                        author   = Author(author),
                        date     = CommitDate(date.trim),
                        message  = CommitMessage(message.mkString.trim)
                      ).asRight
                    ) >> go(rms)
                  case _ =>
                    CmdDecoder.failedAsPull(DecodingFailure.ParsingFailure("Malformed commit log. Malformed details."))
              case None =>
                CmdDecoder.failedAsPull(DecodingFailure.ParsingFailure("Malformed commit log. Missing details."))
            }
          case Some((got, _)) =>
            CmdDecoder.failedAsPull(
              DecodingFailure.ParsingFailure(s"Malformed commit log. Expected commit id but got [$got].")
            )
          case None =>
            Pull.pure(None)
        }

      go(lines).void.stream
    }
