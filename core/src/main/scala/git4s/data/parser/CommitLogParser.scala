package git4s.data.parser

import cats.effect.Async
import fs2.{Pipe, Pull}
import git4s.data.GitCommitLog
import git4s.data.value.{CommitAuthor, CommitAuthorEmail, CommitDate, CommitId, CommitMessage}
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

        inline def buildAndOutputWith(
          authorStr: String,
          merge: Option[(String, String)],
          commitIdStr: String,
          dateStr: String,
          messageLines: List[String]
        ) =
          val (author, maybeEmail) = CommitAuthor.fromString(authorStr)
          Pull.output1(
            GitCommitLog(
              commitId    = CommitId(commitIdStr),
              merge       = merge.map { case (c1, c2) => (CommitId(c1), CommitId(c2)) },
              author      = author,
              authorEmail = maybeEmail,
              date        = CommitDate(dateStr.trim),
              message     = CommitMessage(messageLines.mkString.trim)
            )
          )

        lines.pull.uncons1.flatMap {
          case Some((s"commit $commitIdStr", rms)) =>
            rms.pull.unconsUnless(_.startsWith("commit ")).flatMap {
              case Some((chunk, rms)) =>
                chunk.toList match
                  case s"Author: $authorStr" :: s"Date: $dateStr" :: messageLines =>
                    buildAndOutputWith(
                      authorStr    = authorStr,
                      merge        = None,
                      commitIdStr  = commitIdStr,
                      dateStr      = dateStr,
                      messageLines = messageLines
                    ) >> go(rms)
                  case s"Merge: $c1 $c2" :: s"Author: $authorStr" :: s"Date: $dateStr" :: messageLines =>
                    buildAndOutputWith(
                      authorStr    = authorStr,
                      merge        = Some((c1, c2)),
                      commitIdStr  = commitIdStr,
                      dateStr      = dateStr,
                      messageLines = messageLines
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
