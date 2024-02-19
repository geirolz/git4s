package com.geirolz.git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

case class GitCloneResult(value: String) extends AnyVal
object GitCloneResult:
  given [F[_]: Async]: CmdDecoder[F, GitCloneResult] =
    CmdDecoder.text[F].map(GitCloneResult(_))
