package com.geirolz.git4s

import cats.effect.kernel.Async
import com.geirolz.git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import com.geirolz.git4s.data.GitResetMode
import com.geirolz.git4s.data.value.CommitId
import com.geirolz.git4s.log.CmdLogger

trait Git4sReset[F[_]]:

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

object Git4sReset:
  def apply[F[_]: Async](using WorkingCtx, CmdRunner[F]): Git4sReset[F] = new Git4sReset[F]:

    override def apply()(using CmdLogger[F]): F[Unit] =
      GitCmd.reset.run_

    override def toCommit(mode: GitResetMode, commitId: CommitId)(using CmdLogger[F]): F[Unit] =
      GitCmd.reset.addArgs(mode.asArg, commitId.value).run_

    override def backToNCommit(mode: GitResetMode, n: Int)(using CmdLogger[F]): F[Unit] =
      GitCmd.reset.addArgs(mode.asArg, s"HEAD~$n").run_
