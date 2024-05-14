package com.geirolz.git4s.data.request

import com.geirolz.git4s.data.Arg

sealed trait GitConfigTarget:
  def asArg: Arg = this match
    case GitConfigTarget.Local  => ""
    case GitConfigTarget.Global => "--global"

object GitConfigTarget:
  case object Local extends GitConfigTarget
  case object Global extends GitConfigTarget
