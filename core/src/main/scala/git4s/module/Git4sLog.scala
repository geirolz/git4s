package git4s.module

import cats.effect.kernel.Async
import fs2.Stream
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.{GitCommitLog, GitCommitShortLog}
import git4s.data.value.CommitTag
import git4s.logging.CmdLogger

sealed trait Git4sLog[F[_]]:

  /** Show commit logs.
    *
    * [[https://git-scm.com/docs/git-log]]
    */
  def apply(revisionRange: Option[(CommitTag, CommitTag)] = None)(using CmdLogger[F]): fs2.Stream[F, GitCommitLog]

  /** Summarize git log output
    *
    * [[https://git-scm.com/docs/git-shortlog]]
    */
  def short(
    revisionRange: Option[(CommitTag, CommitTag)] = None,
    email: Boolean                                = true,
    sort: Boolean                                 = false,
    excludeMerges: Boolean                        = false
  )(using CmdLogger[F]): fs2.Stream[F, GitCommitShortLog]

private[git4s] object Git4sLog:

  trait Module[F[_]]:
    def log: Git4sLog[F]

  /** Access the default implementation directly from `Git4s[F].log` */
  def apply[F[_]: Async](using WorkingCtx, CmdRunner[F]): Git4sLog[F] = new Git4sLog[F]:

    override def apply(
      revisionRange: Option[(CommitTag, CommitTag)] = None
    )(using CmdLogger[F]): Stream[F, GitCommitLog] =
      GitCmd
        .log[F]
        .addOptArgs(
          revisionRange.map { case (from, to) => s"$from..$to" }
        )
        .stream

    override def short(
      revisionRange: Option[(CommitTag, CommitTag)] = None,
      email: Boolean                                = true,
      sort: Boolean                                 = false,
      excludeMerges: Boolean                        = false
    )(using CmdLogger[F]): Stream[F, GitCommitShortLog] =
      GitCmd
        .shortLog[F]
        .addOptArgs(
          revisionRange.map { case (from, to) => s"$from..$to" }
        )
        .addFlagArgs(
          email         -> "--email",
          sort          -> "--numbered",
          excludeMerges -> "--no-merges"
        )
        .stream
