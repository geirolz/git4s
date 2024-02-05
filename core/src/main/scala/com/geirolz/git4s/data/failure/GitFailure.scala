package com.geirolz.git4s.data.failure

import cats.Show
import com.geirolz.git4s.cmd.error.CmdFailure
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.data.failure.GitFailure.{GitGenericFailure, NotInAGitRepository}

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

  given CmdDecoder[GitFailure] =
    CmdDecoder.text.flatMap {
      case x if x.startsWith("fatal: not a git repository")   => CmdDecoder.success(NotInAGitRepository)
      case x if x.startsWith("fatal: not in a git directory") => CmdDecoder.success(NotInAGitRepository)
      case other                                              => CmdDecoder.success(GitGenericFailure(other))
    }

  given Show[GitFailure] = Show.fromToString
