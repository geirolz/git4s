package com.geirolz.git4s.data

import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder

case class GitAddResult(value: String) extends AnyVal
object GitAddResult:
  given CmdDecoder[GitAddResult] =
    CmdDecoder.text.map(GitAddResult(_))
