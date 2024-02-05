package com.geirolz.git4s

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import com.geirolz.git4s.data.request.GitConfigTarget
import com.geirolz.git4s.log.CmdLogger
import fs2.io.file.Path

trait Git4sConfig[F[_]](target: GitConfigTarget)(using WorkingCtx):
  def get(key: String)(using CmdLogger[F]): F[Option[String]]
  def set(key: String, value: String)(using CmdLogger[F]): F[Unit]
  def unset(key: String)(using CmdLogger[F]): F[Unit]

object Git4sConfig:

  def local[F[_]: Async: CmdRunner](using WorkingCtx): Git4sConfig[F] =
    apply(GitConfigTarget.Local)

  def global[F[_]: Async: CmdRunner](using WorkingCtx): Git4sConfig[F] =
    apply(GitConfigTarget.Global)

  private def apply[F[_]: Async: CmdRunner](target: GitConfigTarget)(using WorkingCtx): Git4sConfig[F] =
    new Git4sConfig[F](target):

      def get(key: String)(using CmdLogger[F]): F[Option[String]] =
        GitCmd
          .getConfig(target)(key)
          .run
          .map(Option(_).filter(_.nonEmpty))
          .handleError(_ => None)

      def set(key: String, value: String)(using CmdLogger[F]): F[Unit] =
        GitCmd.setConfig(target)(key, value).run

      def unset(key: String)(using CmdLogger[F]): F[Unit] =
        GitCmd.unsetConfig(target)(key).run
