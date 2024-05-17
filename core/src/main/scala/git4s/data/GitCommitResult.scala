package git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.DecodingFailure.GenericDecodingFailure
import git4s.cmd.error.CmdError
import git4s.codec.CmdDecoder

case class GitCommitResult(value: String) extends AnyVal
object GitCommitResult:
  given [F[_]: Async]: CmdDecoder[F, GitCommitResult] =
    CmdDecoder.text[F].map(GitCommitResult(_))
