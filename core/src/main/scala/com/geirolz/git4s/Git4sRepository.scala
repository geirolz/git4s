package com.geirolz.git4s

import cats.effect.kernel.Async
import com.geirolz.git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import com.geirolz.git4s.data.*
import com.geirolz.git4s.data.request.FixupCommit
import com.geirolz.git4s.log.CmdLogger
import fs2.io.file.Path

trait Git4sRepository[F[_]]:

  /** Show the working tree status.
    *
    * [[https://git-scm.com/docs/git-status]]
    */
  def status(
    short: Boolean  = false,
    long: Boolean   = false,
    branch: Boolean = false
  )(using CmdLogger[F]): F[GitStatus]

  /** Create an empty Git repository or reinitialize an existing one.
    *
    * [[https://git-scm.com/docs/git-init]]
    */
  def init(
    quiet: Boolean = false,
    bare: Boolean  = false
  )(using CmdLogger[F]): F[GitInitResult]

  /** Add file contents to the index.
    *
    * Interactive commands not supported yet.
    *
    * [[https://git-scm.com/docs/git-add]]
    */
  def add(
    pattern: String,
    dryRun: Boolean             = false,
    verbose: Boolean            = false,
    force: Boolean              = false,
    sparse: Boolean             = false,
    all: Boolean                = false,
    noAll: Boolean              = false,
    refresh: Boolean            = false,
    ignoreErrors: Boolean       = false,
    ignoreMissing: Boolean      = false,
    noWarnEmbeddedRepo: Boolean = false,
    renormalize: Boolean        = false
  )(using CmdLogger[F]): F[GitAddResult]

  /** Record changes to the repository.
    *
    * [[https://git-scm.com/docs/git-commit]]
    */
  def commit(
    message: String,
    all: Boolean                 = false,
    reuseMessage: Option[String] = None,
    fixup: Option[FixupCommit]   = None,
    squash: Option[String]       = None,
    short: Boolean               = false,
    branch: Boolean              = false
  )(using CmdLogger[F]): F[GitCommitResult]

object Git4sRepository:
  def apply[F[_]: Async](using WorkingCtx, CmdRunner[F]): Git4sRepository[F] = new Git4sRepository[F]:
    override def status(
      short: Boolean  = false,
      long: Boolean   = false,
      branch: Boolean = false
    )(using CmdLogger[F]): F[GitStatus] =
      GitCmd
        .status[F]
        .addFlagArgs(
          short  -> "--short",
          long   -> "--long",
          branch -> "--branch"
        )
        .run

    override def init(
      quiet: Boolean = false,
      bare: Boolean  = false
    )(using CmdLogger[F]): F[GitInitResult] =
      GitCmd
        .init[F]
        .addFlagArgs(
          quiet -> "--quiet",
          bare  -> "--bare"
        )
        .run

    override def add(
      pattern: String,
      dryRun: Boolean             = false,
      verbose: Boolean            = false,
      force: Boolean              = false,
      sparse: Boolean             = false,
      all: Boolean                = false,
      noAll: Boolean              = false,
      refresh: Boolean            = false,
      ignoreErrors: Boolean       = false,
      ignoreMissing: Boolean      = false,
      noWarnEmbeddedRepo: Boolean = false,
      renormalize: Boolean        = false
    )(using CmdLogger[F]): F[GitAddResult] =
      GitCmd
        .add[F](pattern)
        .addFlagArgs(
          dryRun             -> "--dry-run",
          verbose            -> "--verbose",
          force              -> "--force",
          sparse             -> "--sparse",
          all                -> "--all",
          noAll              -> "--no-all",
          refresh            -> "--refresh",
          ignoreErrors       -> "--ignore-errors",
          ignoreMissing      -> "--ignore-missing",
          noWarnEmbeddedRepo -> "--no-warn-embedded-repo",
          renormalize        -> "--renormalize"
        )
        .run

    override def commit(
      message: String,
      all: Boolean                 = false,
      reuseMessage: Option[String] = None,
      fixup: Option[FixupCommit]   = None,
      squash: Option[String]       = None,
      short: Boolean               = false,
      branch: Boolean              = false
    )(using CmdLogger[F]): F[GitCommitResult] =
      GitCmd
        .commit[F](message)
        .addOptArgs(
          reuseMessage.map(c => s"--reuse-message=$c"),
          fixup.map(f => s"--fixup=${f.tpe} ${f.commit}"),
          squash.map(s => s"--squash=$s")
        )
        .addFlagArgs(
          all    -> "--all",
          short  -> "--short",
          branch -> "--branch"
        )
        .run
