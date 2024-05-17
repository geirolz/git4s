package git4s

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.data.*
import fs2.Stream
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.{GitAddResult, GitCommitLog, GitCommitResult, GitInitResult, GitStatus}
import git4s.data.diff.FileDiff
import git4s.data.request.FixupCommit
import git4s.data.value.{Arg, BranchName, Remote}
import git4s.log.CmdLogger

// - tag
// - rename branch
trait Git4sRepository[F[_]]:

  // ==================== BRANCH BASED ====================
  /** Show changes between commits, commit and working tree, etc
    *
    * [[https://git-scm.com/docs/git-diff]]
    */
  def diff(
    pattern: Option[String] = None,
    added: Boolean          = true,
    copied: Boolean         = true,
    deleted: Boolean        = true,
    modified: Boolean       = true,
    renamed: Boolean        = true,
    typeChanged: Boolean    = true,
    unmerged: Boolean       = true,
    unknown: Boolean        = true,
    broken: Boolean         = true,
    allOrNone: Boolean      = true
  )(using CmdLogger[F]): Stream[F, FileDiff]

  /** Return a Git4sReset type to perform resets */
  def reset: Git4sReset[F]

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
    noVerify: Boolean                = false,
    autoStash: Boolean               = false
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

  /** Remove untracked files from the working tree.
    *
    * [[https://git-scm.com/docs/git-clean]]
    */
  def clean(
    force: Boolean                 = false,
    recursive: Boolean             = false,
    dontUseStdIgnoreRules: Boolean = false
  )(using CmdLogger[F]): F[Unit]

  /** Show commit logs.
    *
    * [[https://git-scm.com/docs/git-log]]
    */
  def log: fs2.Stream[F, GitCommitLog]

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

    override def diff(
      pattern: Option[String] = None,
      added: Boolean          = true,
      copied: Boolean         = true,
      deleted: Boolean        = true,
      modified: Boolean       = true,
      renamed: Boolean        = true,
      typeChanged: Boolean    = true,
      unmerged: Boolean       = true,
      unknown: Boolean        = true,
      broken: Boolean         = true,
      allOrNone: Boolean      = false
    )(using CmdLogger[F]): Stream[F, FileDiff] =

      val mayFilter: Option[Arg] =
        List(
          added       -> "A",
          copied      -> "C",
          deleted     -> "D",
          modified    -> "M",
          renamed     -> "R",
          typeChanged -> "T",
          unmerged    -> "U",
          unknown     -> "X",
          broken      -> "B",
          allOrNone   -> "*"
        ).collect { case (true, value) => value }
          .mkString
          .some
          .filterNot(_.isEmpty)
          .map(v => s"--diff-filter=$v")

      GitCmd.diff[F].addOptArgs(mayFilter, pattern).stream

    override def currentBranch(using CmdLogger[F]): F[BranchName] =
      GitCmd.revParse[F].addArgs("--abbrev-ref", "HEAD").runGetLast

    override def status(using CmdLogger[F]): F[GitStatus] =
      GitCmd.status[F].runGetLast

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
          .runGetLast
      } yield ()

    override def pull(
      remote: Remote                   = Remote.origin,
      targetBranch: Option[BranchName] = None,
      rebase: Boolean                  = false,
      fastForwardOnly: Boolean         = false,
      noFastForward: Boolean           = false,
      squash: Boolean                  = false,
      noCommit: Boolean                = false,
      noVerify: Boolean                = false,
      autoStash: Boolean               = false
    )(using CmdLogger[F]): F[Unit] =
      GitCmd
        .pull[F](remote, targetBranch)
        .addFlagArgs(
          rebase          -> "--rebase",
          fastForwardOnly -> "--ff-only",
          noFastForward   -> "--no-ff",
          squash          -> "--squash",
          noCommit        -> "--no-commit",
          noVerify        -> "--no-verify",
          autoStash       -> "--autostash"
        )
        .runGetLast

    override def add(pattern: String = ".")(using CmdLogger[F]): F[GitAddResult] =
      GitCmd.add[F](pattern).runGetLast

    override def commit(
      message: String,
      fixup: Option[FixupCommit] = None
    )(using CmdLogger[F]): F[GitCommitResult] =
      GitCmd
        .commit[F](message)
        .addOptArgs(
          fixup.map(f => "--fixup=${f.tpe} ${f.commit}")
        )
        .runGetLast

    override def clean(
      force: Boolean                 = false,
      recursive: Boolean             = false,
      dontUseStdIgnoreRules: Boolean = false
    )(using CmdLogger[F]): F[Unit] =
      GitCmd
        .clean[F]
        .addFlagArgs(
          force                 -> "-f",
          recursive             -> "-d",
          dontUseStdIgnoreRules -> "-x"
        )
        .run_

    override def log: Stream[F, GitCommitLog] =
      GitCmd.log[F].stream

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
      GitCmd.fetch[F](remote).runGetLast

    override def init(using CmdLogger[F]): F[GitInitResult] =
      GitCmd.init[F].runGetLast
