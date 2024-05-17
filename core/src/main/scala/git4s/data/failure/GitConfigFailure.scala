package git4s.data.failure

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.CmdDecoder

sealed trait GitConfigFailure extends GitFailure
object GitConfigFailure:
  case class KeyNotFound(key: String) extends GitConfigFailure
  case class UnmappedFailure(failure: GitFailure) extends GitConfigFailure, GitFailure.UnmappedFailure(failure)

  given [F[_]: Async]: CmdDecoder[F, GitConfigFailure] =
    CmdDecoder.text[F].map {
      case s"error: key does not contain a section: $key" =>
        KeyNotFound(key)
      case x =>
        UnmappedFailure(GitFailure.parseString(x))
    }
