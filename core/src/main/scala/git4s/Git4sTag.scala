package git4s

import cats.effect.{Async, IO}
import cats.syntax.all.*
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.value.CommitTag

trait Git4sTag[F[_]](using WorkingCtx):
  def list(pattern: Option[String] = None)(using CmdRunner[F]): fs2.Stream[F, CommitTag]
  def exists(tag: CommitTag)(using CmdRunner[F]): F[Boolean]
  def create(tag: CommitTag)(using CmdRunner[F]): F[Unit]
  def replace(tag: CommitTag)(using CmdRunner[F]): F[Unit]
  def delete(tag: CommitTag)(using CmdRunner[F]): F[Unit]

object Git4sTag:
  def apply[F[_]: Async](using WorkingCtx, CmdRunner[F]): Git4sTag[F] = new Git4sTag[F]:

    override def list(pattern: Option[String] = None)(using CmdRunner[F]): fs2.Stream[F, CommitTag] =
      GitCmd.listTag(pattern).stream

    override def exists(tag: CommitTag)(using CmdRunner[F]): F[Boolean] =
      list(Some(tag.value)).compile.last.map(_.isDefined)

    override def create(tag: CommitTag)(using CmdRunner[F]): F[Unit] =
      GitCmd.tag[F].addArgs(tag.value).run_

    override def replace(tag: CommitTag)(using CmdRunner[F]): F[Unit] =
      GitCmd.tag[F].addArgs(s"--force ${tag.value}").run_

    override def delete(tag: CommitTag)(using CmdRunner[F]): F[Unit] =
      GitCmd.tag[F].addArgs(s"--delete ${tag.value}").run_
