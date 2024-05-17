package git4s.data.failure

import cats.Show
import cats.effect.kernel.Async
import GitFailure.{GitGenericFailure, NotInAGitRepository}
import git4s.cmd.error.CmdFailure
import git4s.codec.CmdDecoder

trait GitFailure extends CmdFailure:
  override def toString: String =
    this match
      case GitGenericFailure(message) => s"$message"
      case NotInAGitRepository        => "NotAGitRepositoryOrParentDir"

object GitFailure:
  case class GitGenericFailure(message: String) extends GitFailure
  case object NotInAGitRepository extends GitFailure

  /** This trait is used to define a failure that is not mapped to a specific error message. This will be flattened to a
    * generic failure during the run.
    */
  private[git4s] trait UnmappedFailure(failure: GitFailure) extends GitFailure:
    override def toString: String = failure.toString

  def parseString(s: String): GitFailure =
    s match
      case x if x.startsWith("fatal: not a git repository")   => NotInAGitRepository
      case x if x.startsWith("fatal: not in a git directory") => NotInAGitRepository
      case other                                              => GitGenericFailure(other)

  given [F[_]: Async]: CmdDecoder[F, GitFailure] =
    CmdDecoder.text[F].map(parseString(_))

  given Show[GitFailure] = Show.fromToString
