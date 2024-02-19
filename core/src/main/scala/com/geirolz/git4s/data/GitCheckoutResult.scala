package com.geirolz.git4s.data

import cats.effect.kernel.Async
import com.geirolz.git4s.codec.CmdDecoder

case class GitCheckoutResult(value: String) extends AnyVal
object GitCheckoutResult:
  given [F[_]: Async]: CmdDecoder[F, GitCheckoutResult] =
    CmdDecoder.text[F].map(GitCheckoutResult(_))
