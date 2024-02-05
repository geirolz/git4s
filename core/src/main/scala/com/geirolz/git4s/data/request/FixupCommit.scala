package com.geirolz.git4s.data.request

import com.geirolz.git4s.data.request.FixupCommit.{Amend, Reword}

sealed trait FixupCommit:

  val commit: String

  def tpe: String =
    this match
      case _: Amend  => "amend"
      case _: Reword => "reword"

object FixupCommit:
  case class Amend(commit: String) extends FixupCommit
  case class Reword(commit: String) extends FixupCommit
