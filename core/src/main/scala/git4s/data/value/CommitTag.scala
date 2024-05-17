package git4s.data.value

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.CmdDecoder

opaque type CommitTag = String
object CommitTag extends NewType[String, CommitTag]:
  given [F[_]: Async]: CmdDecoder[F, CommitTag] =
    CmdDecoder.lines(_.filterNot(_.isBlank).map(t => CommitTag(t).asRight))
