package git4s.data.value

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.CmdDecoder

opaque type CommitId = String
object CommitId extends NewType[String, CommitId]

opaque type CommitAuthor = String
object CommitAuthor extends NewType[String, CommitAuthor]:
  def fromString(str: String): (CommitAuthor, Option[CommitAuthorEmail]) =
    str match
      case s"$author <$email>" => (CommitAuthor(author), Some(CommitAuthorEmail(email)))
      case s"$author"          => (CommitAuthor(author), None)

opaque type CommitAuthorEmail = String
object CommitAuthorEmail extends NewType[String, CommitAuthorEmail]

opaque type CommitDate = String
object CommitDate extends NewType[String, CommitDate]

opaque type CommitMessage = String
object CommitMessage extends NewType[String, CommitMessage]

opaque type CommitTag = String
object CommitTag extends NewType[String, CommitTag]:
  given [F[_]: Async]: CmdDecoder[F, CommitTag] =
    CmdDecoder.lines(_.filterNot(_.isBlank).map(t => CommitTag(t).asRight))
