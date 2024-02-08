package com.geirolz.git4s

import cats.effect.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import com.geirolz.git4s.data.GitVersion
import com.geirolz.git4s.log.CmdLogger
import fs2.io.file.Path

sealed trait Git4s[F[_]] extends GitInstaller[F], Git4sRepository[F]:

  // directory
  inline def withWorkingDirectory(dir: String): Git4s[F] = withWorkingDirectory(Path(dir))
  def withWorkingDirectory(dir: Path): Git4s[F]
  def withCurrentWorkingDirectory: Git4s[F]
  def workingDirectory: Option[Path]

  // basic commands
  /** Get the version of the installed git
    *
    * [[https://git-scm.com/docs/git-version]]
    */
  def help(using CmdLogger[F]): F[String]

  /** Clone a repository into a new directory.
    *
    * This method is not under [[Git4sRepository]] because it's not related to a specific repository.
    *
    * [[https://git-scm.com/docs/git-clone]]
    */
  def clone(repository: String, directory: Path)(using CmdLogger[F]): F[Unit]

  /** Get a type to access Git local config */
  def localConfig: Git4sConfig[F]

  /** Get a type to access Git global config */
  def globalConfig: Git4sConfig[F]

object Git4s:

  def apply[F[_]: Async: CmdRunner]: Git4s[F] =
    Git4s[F](
      workingDir = None,
      installer  = GitInstaller.default[F]
    )

  private def apply[F[_]: Async: CmdRunner](
    workingDir: Option[Path] = None,
    installer: GitInstaller[F]
  ): Git4s[F] =
    new Git4s[F]:

      private given WorkingCtx                    = WorkingCtx(workingDir)
      private val repository: Git4sRepository[F]  = Git4sRepository[F]
      override val workingDirectory: Option[Path] = WorkingCtx.current.directory

      export installer.*
      export repository.*

      override def withWorkingDirectory(dir: Path): Git4s[F] =
        Git4s(workingDir = Some(dir), installer)

      override def withCurrentWorkingDirectory: Git4s[F] =
        Git4s(workingDir = None, installer)

      override def help(using CmdLogger[F]): F[String] =
        GitCmd.help.run.map(_.value)

      override def clone(repository: String, directory: Path)(using CmdLogger[F]): F[Unit] =
        GitCmd.clone(repository, directory).run.void

      override def localConfig: Git4sConfig[F] =
        Git4sConfig.local[F]

      override def globalConfig: Git4sConfig[F] =
        Git4sConfig.global[F]
