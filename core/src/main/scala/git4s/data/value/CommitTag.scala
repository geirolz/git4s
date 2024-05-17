package git4s.data.value

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.CmdDecoder
import git4s.codec.DecodingFailure.ParsingFailure
import git4s.data.parser.CommitLogParser

opaque type CommitTag = String
object CommitTag:
  def apply(value: String): CommitTag         = value
  extension (id: CommitTag) def value: String = id

  given [F[_]: Async]: CmdDecoder[F, CommitTag] =
    CmdDecoder.lines(_.filterNot(_.isBlank).map(t => CommitTag(t).asRight))
