package com.geirolz.git4s.cmd

import cats.effect.kernel.Async
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.data.*
import com.geirolz.git4s.data.failure.{GitCheckoutFailure, GitConfigFailure, GitFailure}
import com.geirolz.git4s.data.request.GitConfigTarget
import com.geirolz.git4s.data.value.{BranchName, Remote}
import fs2.io.file.Path

private[git4s] object GitCmd:

  type GitCmd[F[_], E <: GitFailure, T] = Cmd[F, E, T]
  def git[F[_], E <: GitFailure: CmdDecoder, T: CmdDecoder](
    arg1: String,
    args: String*
  ): GitCmd[F, E, T] =
    Cmd[F, E, T]("git", arg1 +: args*)

  /** [[https://git-scm.com/docs/git-help]] */
  def help[F[_]: Async]: GitCmd[F, GitFailure, GitHelp] =
    git("help")

  /** [[https://git-scm.com/docs/git-clone]] */
  def clone[F[_]: Async](repository: String, destination: Path): GitCmd[F, GitFailure, GitCloneResult] =
    git("clone", repository, destination.toString)

  /** [[https://git-scm.com/docs/git-version]] */
  def version[F[_]: Async]: GitCmd[F, GitFailure, GitVersion] =
    git("version")

  /** [[https://git-scm.com/docs/git-init]] */
  def init[F[_]: Async]: GitCmd[F, GitFailure, GitInitResult] =
    git("init")

  /** [[https://git-scm.com/docs/git-pull]] */
  def pull[F[_]: Async](
    remote: Remote             = Remote.origin,
    branch: Option[BranchName] = None
  ): Cmd[F, GitFailure, Unit] =
    git("pull", remote).addOptArgs(branch)

    /** [[https://git-scm.com/docs/git-push]] */
  def push[F[_]: Async](remote: Remote = Remote.origin): Cmd[F, GitFailure, Unit] =
    git("push", remote)

  /** [[https://git-scm.com/docs/git-clean]] */
  def clean[F[_] : Async]: Cmd[F, GitFailure, String] =
    git("clean")
    
  /** [[https://git-scm.com/docs/git-branch]] */
  def branch[F[_]: Async]: Cmd[F, GitFailure, String] =
    git("branch")
  
  /** [[https://git-scm.com/docs/git-fetch]] */
  def checkout[F[_]: Async]: Cmd[F, GitCheckoutFailure, String] =
    git("checkout")

  /** [[https://git-scm.com/docs/git-fetch]] */
  def fetch[F[_]: Async](remote: Remote = Remote.origin): Cmd[F, GitFailure, Unit] =
    git("fetch", remote)

  /** [[https://git-scm.com/docs/git-status]] */
  def rev[F[_]: Async]: Cmd[F, GitFailure, String] =
    git("rev")

  /** [[https://git-scm.com/docs/git-status]] */
  def status[F[_]: Async]: Cmd[F, GitFailure, GitStatus] =
    git("status")

  /** [[https://git-scm.com/docs/git-add]] */
  def add[F[_]: Async](pattern: String): GitCmd[F, GitFailure, GitAddResult] =
    git("add", pattern)

  /** [[https://git-scm.com/docs/git-reset]] */
  def reset[F[_]: Async]: GitCmd[F, GitFailure, String] =
    git("reset")

  /** [[https://git-scm.com/docs/git-commit]] */
  def commit[F[_]: Async](message: String): GitCmd[F, GitFailure, GitCommitResult] =
    git("commit", s"-m $message")

  // ============================ Config ============================
  /** [[https://git-scm.com/docs/git-config]] */
  def config[F[_], T: CmdDecoder]: GitCmd[F, GitConfigFailure, T] =
    git("config")

  /** [[https://git-scm.com/docs/git-config]] */
  def getConfig[F[_]: Async](target: GitConfigTarget)(key: String): GitCmd[F, GitConfigFailure, String] =
    config.addArgs("--get", target.asArg, key)

  /** [[https://git-scm.com/docs/git-config]] */
  def setConfig[F[_]: Async](
    target: GitConfigTarget
  )(key: String, value: String): GitCmd[F, GitConfigFailure, Unit] =
    config.addArgs(target.asArg, key, value)

  /** [[https://git-scm.com/docs/git-config]] */
  def unsetConfig[F[_]: Async](target: GitConfigTarget)(key: String): GitCmd[F, GitConfigFailure, Unit] =
    config.addArgs(target.asArg, "--unset", key)
