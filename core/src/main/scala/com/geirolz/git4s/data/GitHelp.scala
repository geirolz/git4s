package com.geirolz.git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

case class GitHelp(value: String) extends AnyVal
object GitHelp:
  given [F[_]: Async]: CmdDecoder[F, GitHelp] =
    CmdDecoder.text[F].map(GitHelp(_))
