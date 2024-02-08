package com.geirolz.git4s.data.failure

import com.geirolz.git4s.codec.CmdDecoder

import scala.annotation.targetName
import cats.syntax.all.*

sealed trait GitConfigFailure extends GitFailure
object GitConfigFailure:
  case class KeyNotFound(key: String) extends GitConfigFailure
  case class UnmappedFailure(failure: GitFailure) extends GitConfigFailure, GitFailure.UnmappedFailure(failure)

  given CmdDecoder[GitConfigFailure] =
    CmdDecoder.text.flatMap {
      case s"error: key does not contain a section: $key" =>
        CmdDecoder.success(KeyNotFound(key))
      case _ =>
        CmdDecoder[GitFailure].map(UnmappedFailure(_))
    }
