package git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.CmdDecoder

case class GitAddResult(value: String) extends AnyVal
object GitAddResult:
  given [F[_]: Async]: CmdDecoder[F, GitAddResult] =
    CmdDecoder.text[F].map(GitAddResult(_))
