package git4s.module

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.request.GitConfigTarget
import git4s.data.request.GitConfigTarget.{Global, Local}
import git4s.logging.CmdLogger

sealed trait Git4sConfig[F[_]](target: GitConfigTarget)(using WorkingCtx):

  /** Get the value of a key in the config */
  def get(key: String)(using CmdLogger[F]): F[Option[String]]

  /** Set the value of a key in the config */
  def set(key: String, value: String)(using CmdLogger[F]): F[Unit]

  /** Unset the value of a key in the config */
  def unset(key: String)(using CmdLogger[F]): F[Unit]

private[git4s] object Git4sConfig:

  trait Module[F[_]]:
    def reset: Git4sConfig[F]

  /** Access the default implementation directly from `Git4s[F].localConfig` */
  def local[F[_]: Async: CmdRunner](using WorkingCtx): Git4sConfig[F] =
    apply(Local)

  /** Access the default implementation directly from `Git4s[F].globalConfig` */
  def global[F[_]: Async: CmdRunner](using WorkingCtx): Git4sConfig[F] =
    apply(Global)

  private def apply[F[_]: Async: CmdRunner](target: GitConfigTarget)(using WorkingCtx): Git4sConfig[F] =
    new Git4sConfig[F](target):

      def get(key: String)(using CmdLogger[F]): F[Option[String]] =
        GitCmd
          .getConfig(target)(key)
          .runGetLast
          .map(Option(_).filter(_.nonEmpty))
          .handleError(_ => None)

      def set(key: String, value: String)(using CmdLogger[F]): F[Unit] =
        GitCmd.setConfig(target)(key, value).runGetLast

      def unset(key: String)(using CmdLogger[F]): F[Unit] =
        GitCmd.unsetConfig(target)(key).runGetLast
