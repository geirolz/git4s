package com.geirolz.git4s.cmd.error

import cats.effect.kernel.Async
import com.geirolz.git4s.codec.CmdDecoder

import scala.util.control.NoStackTrace

final case class CmdError(message: String, cause: Option[Throwable] = None)
    extends RuntimeException(message, cause.orNull)
    with NoStackTrace
object CmdError:
  given CmdDecoder[CmdError] =
    CmdDecoder.text.map(CmdError(_))
