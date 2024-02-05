package com.geirolz.git4s.data

import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

case class GitCloneResult(value: String) extends AnyVal
object GitCloneResult:
  given CmdDecoder[GitCloneResult] =
    CmdDecoder.text.map(GitCloneResult(_))
