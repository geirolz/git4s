package com.geirolz.git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

case class GitAddResult(value: String) extends AnyVal
object GitAddResult:
  given [F[_]: Async]: CmdDecoder[F, GitAddResult] =
    CmdDecoder.text[F].map(GitAddResult(_))
