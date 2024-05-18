package git4s.module

import cats.effect.kernel.Async
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.GitResetMode
import git4s.data.value.{Arg, CommitId}
import git4s.logging.CmdLogger

sealed trait Git4sReset[F[_]]:

  /** Reset the current HEAD to the specified state. */
  def apply()(using CmdLogger[F]): F[Unit]

  /** Reset the current HEAD to the specified state.
    * @param mode
    *   the mode of the reset
    * @param commitId
    *   the commit to reset to
    */
  def toCommit(mode: GitResetMode, commitId: CommitId)(using CmdLogger[F]): F[Unit]

  /** Reset the current HEAD to the specified state.
    * @param mode
    *   the mode of the reset
    * @param n
    *   the number of commits to go back
    */
  def backToNCommit(mode: GitResetMode, n: Int)(using CmdLogger[F]): F[Unit]

private[git4s] object Git4sReset:

  trait Module[F[_]]:
    /** Return a Git4sReset type to perform resets */
    def reset: Git4sReset[F]

  /** Access the default implementation directly from `Git4s[F].reset` */
  def apply[F[_]: Async](using WorkingCtx, CmdRunner[F]): Git4sReset[F] = new Git4sReset[F]:

    override def apply()(using CmdLogger[F]): F[Unit] =
      GitCmd.reset.run_

    override def toCommit(mode: GitResetMode, commitId: CommitId)(using CmdLogger[F]): F[Unit] =
      GitCmd.reset.addArgs(mode.asArg, commitId.value).run_

    override def backToNCommit(mode: GitResetMode, n: Int)(using CmdLogger[F]): F[Unit] =
      GitCmd.reset.addArgs(mode.asArg, "HEAD~$n").run_
