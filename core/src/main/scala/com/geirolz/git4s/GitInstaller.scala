package com.geirolz.git4s

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.{Cmd, CmdRunner, GitCmd, WorkingCtx}
import com.geirolz.git4s.data.{GitVersion, OperativeSystem}
import com.geirolz.git4s.log.CmdLogger

trait GitInstaller[F[_]]:

  /** Install git */
  def install(using CmdLogger[F]): F[Unit]

  /** Install git if it's not already installed */
  def installIfNeeded(using CmdLogger[F]): F[Boolean]

  /** Uninstall git */
  def uninstall(using CmdLogger[F]): F[Unit]

  /** Uninstall git and then install it again */
  def reinstall(using CmdLogger[F]): F[Unit]

  /** Check if git is installed */
  def isInstalled(using CmdLogger[F]): F[Boolean]

  /** Get the installed git version */
  def version(using CmdLogger[F]): F[GitVersion]

object GitInstaller:

  /** Create a new instance of GitInstaller using the default package manager for the current operative system
    *   - MacOS: Brew
    *   - Windows: Choco
    *   - Linux: Apt-get
    */
  def default[F[_]: Async: CmdRunner]: GitInstaller[F] = {

    import WorkingCtx.global.given

    def installWith(app: String)(using CmdLogger[F]): F[Unit] =
      Cmd.simple_(app, "install", "git").runGetLast

    def uninstallWith(app: String)(using CmdLogger[F]): F[Unit] =
      Cmd.simple_(app, "uninstall", "git").runGetLast

    GitInstaller.instance[F](
      installF = {
        case OperativeSystem.Linux   => installWith("apt-get")
        case OperativeSystem.MacOS   => installWith("brew")
        case OperativeSystem.Windows => installWith("choco")
      },
      uninstallF = {
        case OperativeSystem.Linux   => uninstallWith("apt-get")
        case OperativeSystem.MacOS   => uninstallWith("brew")
        case OperativeSystem.Windows => uninstallWith("choco")
      }
    )
  }

  def instance[F[_]: Async: CmdRunner](
    installF: CmdLogger[F] ?=> OperativeSystem => F[Unit],
    uninstallF: CmdLogger[F] ?=> OperativeSystem => F[Unit]
  ): GitInstaller[F] =
    new GitInstaller[F]:

      import WorkingCtx.global.given

      override def install(using CmdLogger[F]): F[Unit] =
        OperativeSystem.getCurrent[F].flatMap(installF)

      override def installIfNeeded(using CmdLogger[F]): F[Boolean] =
        isInstalled.ifM(false.pure[F], install.as(true))

      override def uninstall(using CmdLogger[F]): F[Unit] =
        OperativeSystem.getCurrent[F].flatMap(uninstallF)

      override def reinstall(using CmdLogger[F]): F[Unit] =
        uninstall >> install

      override def isInstalled(using CmdLogger[F]): F[Boolean] =
        version.attempt.map(_.isRight)

      override def version(using CmdLogger[F]): F[GitVersion] =
        GitCmd.version[F].runGetLast
