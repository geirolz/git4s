package com.geirolz.git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.CmdError
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.codec.DecodingFailure.GenericDecodingFailure

case class GitCommitResult(value: String) extends AnyVal
object GitCommitResult:
  given [F[_]: Async]: CmdDecoder[F, GitCommitResult] =
    CmdDecoder.text[F].map(GitCommitResult(_))
