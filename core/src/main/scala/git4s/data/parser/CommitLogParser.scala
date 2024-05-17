package git4s.data.parser

import cats.effect.Async
import fs2.{Pipe, Pull}
import git4s.data.GitCommitLog
import git4s.data.value.{CommitAuthor, CommitDate, CommitId, CommitMessage}
import git4s.utils.*

trait CommitLogParser[F[_]]:
  def parse: Pipe[F, String, GitCommitLog]
object CommitLogParser:
  def apply[F[_]](using p: CommitLogParser[F]): CommitLogParser[F] = p

  given [F[_]](using F: Async[F]): CommitLogParser[F] with
    def parse: Pipe[F, String, GitCommitLog] = (lines: fs2.Stream[F, String]) => {

      def go(lines: fs2.Stream[F, String]): Pull[F, GitCommitLog, Option[fs2.Stream[F, String]]] =

        inline def failWith(msg: String): Pull[F, Nothing, Nothing] =
          Pull.raiseError(new RuntimeException(msg))

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
                        author   = CommitAuthor(author),
                        date     = CommitDate(date.trim),
                        message  = CommitMessage(message.mkString.trim)
                      )
                    ) >> go(rms)
                  case s"Merge: $c1 $c2" :: s"Author: $author" :: s"Date: $date" :: message =>
                    Pull.output1(
                      GitCommitLog(
                        commitId = CommitId(commitId),
                        merge    = Some((CommitId(c1), CommitId(c2))),
                        author   = CommitAuthor(author),
                        date     = CommitDate(date.trim),
                        message  = CommitMessage(message.mkString.trim)
                      )
                    ) >> go(rms)
                  case _ =>
                    failWith("Malformed commit log. Malformed details.")
              case None =>
                failWith("Malformed commit log. Missing details.")
            }

          // skip line
          case Some((got, rms)) =>
            go(rms)

          // no more elements
          case None =>
            Pull.pure(None)
        }

      go(lines).void.stream
    }
