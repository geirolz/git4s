package git4s.cmd.error

import cats.effect.kernel.Async
import git4s.codec.CmdDecoder

import scala.util.control.NoStackTrace

final case class CmdError(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message, cause.orNull)
    with NoStackTrace
object CmdError:
  given [F[_]: Async]: CmdDecoder[F, CmdError] =
    CmdDecoder.text[F].map(CmdError(_))
