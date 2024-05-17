package git4s.data.request

import git4s.data.value.Arg

sealed trait GitConfigTarget:
  def asArg: Arg = this match
    case GitConfigTarget.Local  => ""
    case GitConfigTarget.Global => "--global"

object GitConfigTarget:
  case object Local extends GitConfigTarget
  case object Global extends GitConfigTarget
