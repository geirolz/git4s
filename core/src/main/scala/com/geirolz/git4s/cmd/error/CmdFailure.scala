package com.geirolz.git4s.cmd.error

import cats.Show
import cats.syntax.show.*

import scala.util.control.NoStackTrace

trait CmdFailure extends NoStackTrace
object CmdFailure:
  def apply[E: Show](failure: E): CmdFailure =
    new CmdFailure:
      override def getMessage: String = failure.show
