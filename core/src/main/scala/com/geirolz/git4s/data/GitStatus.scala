package com.geirolz.git4s.data

import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

sealed trait GitStatus
object GitStatus:
  case object NothingToCommit extends GitStatus
  case class GitStatusGeneric(value: String) extends GitStatus
  given CmdDecoder[GitStatus] =
    CmdDecoder.text.map {
      case x if x.startsWith("nothing to commit") => NothingToCommit
      case other                                  => GitStatusGeneric(other)
    }
