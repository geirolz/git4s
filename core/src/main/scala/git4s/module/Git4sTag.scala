package git4s.module

import cats.effect.{Async, IO}
import cats.syntax.all.*
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.value.CommitTag

sealed trait Git4sTag[F[_]](using WorkingCtx):
  def list(pattern: Option[String] = None)(using CmdRunner[F]): fs2.Stream[F, CommitTag]
  def exists(tag: CommitTag)(using CmdRunner[F]): F[Boolean]
  def create(tag: CommitTag)(using CmdRunner[F]): F[Unit]
  def replace(tag: CommitTag)(using CmdRunner[F]): F[Unit]
  def delete(tag: CommitTag)(using CmdRunner[F]): F[Unit]

private[git4s] object Git4sTag:

  trait Module[F[_]]:
    /** Return a Git4sTag type to perform tag operations */
    def tag: Git4sTag[F]

  /** Access from `Git4s[F].tag` */
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
