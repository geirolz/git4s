package git4s.codec

import scala.util.control.NoStackTrace

trait DecodingFailure extends NoStackTrace
object DecodingFailure:
  def apply(message: String): DecodingFailure = GenericDecodingFailure(message)

  case class GenericDecodingFailure(message: String) extends DecodingFailure:
    override def toString: String = s"DecodingFailure: $message"

  case class ParsingFailure(message: String) extends DecodingFailure:
    override def toString: String = s"ParsingFailure: $message"
