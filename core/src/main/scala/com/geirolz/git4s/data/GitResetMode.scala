package com.geirolz.git4s.data

sealed trait GitResetMode:
  def asArg: Arg = this match
    case GitResetMode.Soft  => "--soft"
    case GitResetMode.Mixed => "--mixed"
    case GitResetMode.Hard  => "--hard"
    case GitResetMode.Merge => "--merge"
    case GitResetMode.Keep  => "--keep"

object GitResetMode:
  case object Soft extends GitResetMode
  case object Mixed extends GitResetMode
  case object Hard extends GitResetMode
  case object Merge extends GitResetMode
  case object Keep extends GitResetMode
