package com.geirolz.git4s.data.request

sealed trait GitConfigTarget:
  def asArg: String = this match
    case GitConfigTarget.Local  => ""
    case GitConfigTarget.Global => "--global"

object GitConfigTarget:
  case object Local extends GitConfigTarget
  case object Global extends GitConfigTarget
