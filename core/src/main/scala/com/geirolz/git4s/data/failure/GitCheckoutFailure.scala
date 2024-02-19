package com.geirolz.git4s.data.failure

import cats.effect.kernel.Async
import com.geirolz.git4s.codec.CmdDecoder

sealed trait GitCheckoutFailure extends GitFailure
object GitCheckoutFailure:
  case class MissingBranchName(message: String) extends GitCheckoutFailure
  case class UnmappedFailure(failure: GitFailure) extends GitCheckoutFailure, GitFailure.UnmappedFailure(failure)

  given [F[_]: Async]: CmdDecoder[F, GitCheckoutFailure] =
    CmdDecoder.text[F].map {
      case x if x.startsWith(s"fatal: missing branch name;") =>
        MissingBranchName(x)
      case x =>
        UnmappedFailure(GitFailure.parseString(x))
    }
