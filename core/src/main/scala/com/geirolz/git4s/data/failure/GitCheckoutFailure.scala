package com.geirolz.git4s.data.failure

import com.geirolz.git4s.codec.CmdDecoder

sealed trait GitCheckoutFailure extends GitFailure
object GitCheckoutFailure:
  case class MissingBranchName(message: String) extends GitCheckoutFailure
  case class UnmappedFailure(failure: GitFailure) extends GitCheckoutFailure, GitFailure.UnmappedFailure(failure)

  given CmdDecoder[GitCheckoutFailure] =
    CmdDecoder.text.flatMap {
      case x if x.startsWith(s"fatal: missing branch name;") =>
        CmdDecoder.success(MissingBranchName(x))
      case _ =>
        CmdDecoder[GitFailure].map(UnmappedFailure(_))
    }
