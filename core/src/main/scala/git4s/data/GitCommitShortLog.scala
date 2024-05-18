package git4s.data

import cats.effect.kernel.Async
import git4s.codec.CmdDecoder
import git4s.data.parser.{CommitLogParser, CommitShortLogParser}
import git4s.data.value.{CommitAuthor, CommitAuthorEmail, CommitMessage}
import cats.syntax.all.*
import git4s.codec.DecodingFailure.ParsingFailure

case class GitCommitShortLog(
  author: CommitAuthor,
  authorEmail: Option[CommitAuthorEmail],
  numberOfCommits: Int,
  commitMessages: List[CommitMessage]
)
object GitCommitShortLog:

  given [F[_]: Async: CommitShortLogParser]: CmdDecoder[F, GitCommitShortLog] =
    CmdDecoder.lines(stream =>
      CommitShortLogParser[F]
        .parse(stream)
        .attempt
        .map(_.leftMap(e => ParsingFailure(e.getMessage)))
    )
