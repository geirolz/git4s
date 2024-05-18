package git4s.data.parser

import cats.effect.Async
import fs2.{Pipe, Pull}
import git4s.data.GitCommitShortLog
import git4s.data.value.{CommitAuthor, CommitAuthorEmail, CommitMessage}

trait CommitShortLogParser[F[_]]:
  def parse: Pipe[F, String, GitCommitShortLog]

object CommitShortLogParser:
  def apply[F[_]](using p: CommitShortLogParser[F]): CommitShortLogParser[F] = p

  given [F[_]](using F: Async[F]): CommitShortLogParser[F] with
    def parse: Pipe[F, String, GitCommitShortLog] = (lines: fs2.Stream[F, String]) => {

      def go(lines: fs2.Stream[F, String]): Pull[F, GitCommitShortLog, Option[fs2.Stream[F, String]]] =

        inline def failWith(msg: String): Pull[F, Nothing, Nothing] =
          Pull.raiseError(new RuntimeException(msg))

        inline def buildOutputAndRepeatWith(
          numberOfCommitsStr: String,
          rms: fs2.Stream[F, String]
        )(author: String, email: Option[String]): Pull[F, GitCommitShortLog, Option[fs2.Stream[F, String]]] =
          numberOfCommitsStr.toIntOption match
            case Some(numberOfCommits) =>
              rms.pull.unconsN(numberOfCommits).flatMap {
                case Some((commits, rms)) =>
                  Pull.output1(
                    GitCommitShortLog(
                      author          = CommitAuthor(author),
                      authorEmail     = email.map(CommitAuthorEmail(_)),
                      numberOfCommits = numberOfCommits,
                      commitMessages  = commits.map(CommitMessage(_)).toList
                    )
                  ) >> go(rms)
                case None =>
                  failWith(s"Expected $numberOfCommits commits.")
              }
            case None =>
              failWith(s"Expected a number of commits, got: $numberOfCommitsStr")

        lines.pull.uncons1.flatMap {
          case Some((s"$author ($numberOfCommitsStr):", rms)) =>
            buildOutputAndRepeatWith(numberOfCommitsStr, rms)(author, None)
          case Some((s"$author <$email> ($numberOfCommitsStr):", rms)) =>
            buildOutputAndRepeatWith(numberOfCommitsStr, rms)(author, Some(email))

          // skip line
          case Some((got, rms)) =>
            go(rms)

          // no more elements
          case None =>
            Pull.pure(None)
        }

      go(lines).void.stream
    }
