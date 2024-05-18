package git4s.data

import cats.effect.kernel.Async
import fs2.{Chunk, Pull}
import git4s.utils.*
import cats.syntax.all.*
import git4s.codec.DecodingFailure.ParsingFailure
import git4s.codec.{CmdDecoder, DecodingFailure}
import git4s.data.diff.FileDiff
import git4s.data.parser.{CommitLogParser, FileDiffParser}
import git4s.data.value.{CommitAuthorId, CommitDate, CommitId, CommitMessage}

case class GitCommitLog(
                         commitId: CommitId,
                         merge: Option[(CommitId, CommitId)],
                         author: CommitAuthorId,
                         date: CommitDate,
                         message: CommitMessage
)
object GitCommitLog:

  given [F[_]: Async: CommitLogParser]: CmdDecoder[F, GitCommitLog] =
    CmdDecoder.lines(stream =>
      CommitLogParser[F]
        .parse(stream)
        .attempt
        .map(_.leftMap(e => ParsingFailure(e.getMessage)))
    )
