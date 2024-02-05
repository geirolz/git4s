package com.geirolz.git4s.data

import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

case class GitHelp(value: String) extends AnyVal
object GitHelp:
  given CmdDecoder[GitHelp] =
    CmdDecoder.text.map(GitHelp(_))
