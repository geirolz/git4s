package com.geirolz.git4s

import cats.effect.kernel.Async
import com.geirolz.git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import com.geirolz.git4s.data.*
import com.geirolz.git4s.data.request.FixupCommit
import com.geirolz.git4s.data.value.{BranchName, CommitId, Remote}
import com.geirolz.git4s.log.CmdLogger
import cats.syntax.all.*

// - tag
// - rename branch
trait Git4sRepository[F[_]]:

  /** Return a Git4sReset type to perform resets */
  def reset: Git4sReset[F]

  // ==================== BRANCH BASED ====================
  /** Show the current branch.
    *
    * [[https://git-scm.com/docs/git-rev-parse]]
    */
  def currentBranch(using CmdLogger[F]): F[BranchName]

  /** Show the working tree status.
    *
    * [[https://git-scm.com/docs/git-status]]
    */
  def status(using CmdLogger[F]): F[GitStatus]

  /** Update remote refs along with associated objects.
    *
    * [[https://git-scm.com/docs/git-push]]
    */
  def push(
    remote: Remote                         = Remote.origin,
    sourceBranch: Option[BranchName]       = None,
    remoteTargetBranch: Option[BranchName] = None,
    force: Boolean                         = false
  )(using CmdLogger[F]): F[Unit]

  /** Fetch from and integrate with another repository or a local branch.
    *
    * [[https://git-scm.com/docs/git-pull]]
    */
  def pull(
    remote: Remote                   = Remote.origin,
    targetBranch: Option[BranchName] = None,
    rebase: Boolean                  = false,
    fastForwardOnly: Boolean         = false,
    noFastForward: Boolean           = false,
    squash: Boolean                  = false,
    noCommit: Boolean                = false,
    noVerify: Boolean                = false
  )(using CmdLogger[F]): F[Unit]

  /** Add file contents to the index.
    *
    * Interactive commands not supported yet.
    *
    * [[https://git-scm.com/docs/git-add]]
    */
  def add(pattern: String = ".")(using CmdLogger[F]): F[GitAddResult]

  /** Record changes to the repository.
    *
    * [[https://git-scm.com/docs/git-commit]]
    */
  def commit(
    message: String,
    fixup: Option[FixupCommit] = None
  )(using CmdLogger[F]): F[GitCommitResult]

  // ==================== BRANCH AGNOSTIC ====================
  /** Select the branch with the specified name */
  def checkout(branchName: BranchName, createIfNotExists: Boolean = true): F[Unit]

  /** Delete a branch.
    *
    * [[https://git-scm.com/docs/git-branch]]
    */
  def deleteLocalBranch(branchName: BranchName): F[Unit]

  /** Delete a branch on the remote.
    *
    * [[https://git-scm.com/docs/git-push]]
    */
  def deleteRemoteBranch(branchName: BranchName, remote: Remote = Remote.origin): F[Unit]

  /** Download objects and refs from another repository.
    *
    * [[https://git-scm.com/docs/git-fetch]]
    */
  def fetch(remote: Remote = Remote.origin): F[Unit]

  /** Create an empty Git repository or reinitialize an existing one.
    *
    * [[https://git-scm.com/docs/git-init]]
    */
  def init(using CmdLogger[F]): F[GitInitResult]

object Git4sRepository:
  def apply[F[_]: Async](using WorkingCtx, CmdRunner[F]): Git4sRepository[F] = new Git4sRepository[F]:

    override lazy val reset: Git4sReset[F] = Git4sReset[F]

    override def currentBranch(using CmdLogger[F]): F[BranchName] =
      GitCmd.rev[F].addArgs("--parse", "--abbrev-ref", "HEAD").run

    override def status(using CmdLogger[F]): F[GitStatus] =
      GitCmd.status[F].run

    override def push(
      remote: Remote                   = Remote.origin,
      sourceBranch: Option[BranchName] = None,
      remoteBranch: Option[BranchName] = None,
      force: Boolean                   = false
    )(using CmdLogger[F]): F[Unit] =
      for {
        srcDstOption <- (sourceBranch, remoteBranch) match
          case (None, None)           => None.pure[F]
          case (Some(src), None)      => s"$src:$src".some.pure[F]
          case (None, Some(dst))      => currentBranch.map(src => s"$src:$dst".some)
          case (Some(src), Some(dst)) => s"$src:$dst".some.pure[F]

        _ <- GitCmd
          .push[F](remote)
          .addOptArgs(srcDstOption)
          .addFlagArgs(force -> "--force")
          .run
      } yield ()

    override def pull(
      remote: Remote                   = Remote.origin,
      targetBranch: Option[BranchName] = None,
      rebase: Boolean                  = false,
      fastForwardOnly: Boolean         = false,
      noFastForward: Boolean           = false,
      squash: Boolean                  = false,
      noCommit: Boolean                = false,
      noVerify: Boolean                = false
    )(using CmdLogger[F]): F[Unit] =
      GitCmd
        .pull[F](remote, targetBranch)
        .addFlagArgs(
          rebase          -> "--rebase",
          fastForwardOnly -> "--ff-only",
          noFastForward   -> "--no-ff",
          squash          -> "--squash",
          noCommit        -> "--no-commit",
          noVerify        -> "--no-verify"
        )
        .run

    override def add(pattern: String = ".")(using CmdLogger[F]): F[GitAddResult] =
      GitCmd.add[F](pattern).run

    override def commit(
      message: String,
      fixup: Option[FixupCommit] = None
    )(using CmdLogger[F]): F[GitCommitResult] =
      GitCmd
        .commit[F](message)
        .addOptArgs(
          fixup.map(f => s"--fixup=${f.tpe} ${f.commit}")
        )
        .run

    override def checkout(
      branchName: BranchName,
      createIfNotExists: Boolean = true
    ): F[Unit] =
      GitCmd
        .checkout[F]
        .addFlagArgs(
          createIfNotExists  -> "-b",
          !createIfNotExists -> "-t"
        )
        .addArgs(branchName)
        .run_

    override def deleteLocalBranch(branchName: BranchName): F[Unit] =
      GitCmd.branch[F].addArgs("-d", branchName).run_

    override def deleteRemoteBranch(branchName: BranchName, remote: Remote = Remote.origin): F[Unit] =
      GitCmd.push[F](remote).addArgs("--delete", branchName).run_

    override def fetch(remote: Remote = Remote.origin): F[Unit] =
      GitCmd.fetch[F](remote).run

    override def init(using CmdLogger[F]): F[GitInitResult] =
      GitCmd.init[F].run
