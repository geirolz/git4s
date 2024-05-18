package git4s

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.data.value.CmdArg.cmd
import git4s.cmd.{Cmd, CmdRunner, GitCmd, WorkingCtx}
import git4s.data.{GitVersion, OperativeSystem}
import git4s.data.OperativeSystem.{Linux, MacOS, Windows}
import git4s.data.value.CmdArg
import git4s.log.CmdLogger

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

/** Access the default implementation directly from `Git4s[F]` or create it with a custom instance */
object GitInstaller:

  /** Create a new instance of GitInstaller using the default package manager for the current operative system
    *   - MacOS: Brew
    *   - Windows: Choco
    *   - Linux: Apt-get
    */
  def default[F[_]: Async: CmdRunner]: GitInstaller[F] = {

    import WorkingCtx.global.given

    def installWith(app: CmdArg)(using CmdLogger[F]): F[Unit] =
      Cmd.simple_(app, "install", "git").runGetLast

    def uninstallWith(app: CmdArg)(using CmdLogger[F]): F[Unit] =
      Cmd.simple_(app, "uninstall", "git").runGetLast

    GitInstaller.instance[F](
      installF = {
        case Linux   => installWith(cmd"apt-get")
        case MacOS   => installWith(cmd"brew")
        case Windows => installWith(cmd"choco")
      },
      uninstallF = {
        case OperativeSystem.Linux   => uninstallWith(cmd"apt-get")
        case OperativeSystem.MacOS   => uninstallWith(cmd"brew")
        case OperativeSystem.Windows => uninstallWith(cmd"choco")
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
