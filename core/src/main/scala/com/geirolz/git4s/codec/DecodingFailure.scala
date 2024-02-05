package com.geirolz.git4s.codec

import scala.util.control.NoStackTrace

trait DecodingFailure extends NoStackTrace
object DecodingFailure:
  case class GenericDecodingFailure(message: String) extends DecodingFailure
