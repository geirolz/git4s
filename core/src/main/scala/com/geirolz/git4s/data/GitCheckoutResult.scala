package com.geirolz.git4s.data

import com.geirolz.git4s.codec.CmdDecoder

case class GitCheckoutResult(value: String) extends AnyVal
object GitCheckoutResult:
  given CmdDecoder[GitCheckoutResult] =
    CmdDecoder.text.map(GitCheckoutResult(_))
