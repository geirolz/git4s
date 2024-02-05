package com.geirolz.git4s.cmd

import cats.{Functor, Show}
import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.CmdError
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.log.CmdLogger
import fs2.Stream
import fs2.io.file.Path

private[git4s] case class Cmd[F[_], E, T](
  decoder: CmdDecoder[T],
  errorDecoder: CmdDecoder[E],
  command: String,
  args: List[String]    = List.empty,
  in: Stream[F, String] = Stream.empty
):

  def setArgs(args: String*): Cmd[F, E, T] =
    copy(args = args.toList.filter(_.nonEmpty))

  def addArgs(args: String*): Cmd[F, E, T] =
    setArgs(this.args ++ args*)

  def addOptArgs(args: Option[String]*): Cmd[F, E, T] =
    addArgs(args.flatten.toList*)

  def addFlagArgs(flags: (Boolean, String)*): Cmd[F, E, T] =
    addArgs(flags.collect { case (true, flag) => flag }*)

  def withInput(input: Stream[F, String]): Cmd[F, E, T] =
    copy(in = input)

  override def toString: String =
    (command :: args).mkString(" ")

  def run(using WorkingCtx, CmdRunner[F], CmdLogger[F]): F[T]                 = CmdRunner[F].run(this)
  def run_(using WorkingCtx, CmdRunner[F], Functor[F], CmdLogger[F]): F[Unit] = run.void

private[git4s] object Cmd:

  def simple_[F[_]: Async](command: String, args: String*): Cmd[F, CmdError, Unit] =
    apply[F, CmdError, Unit](command, args: _*)

  def simple[F[_]: Async](command: String, args: String*): Cmd[F, CmdError, String] =
    apply[F, CmdError, String](command, args: _*)

  def apply[F[_], E: CmdDecoder, T: CmdDecoder](
    command: String,
    args: String*
  ): Cmd[F, E, T] =
    new Cmd(
      decoder      = CmdDecoder[T],
      errorDecoder = CmdDecoder[E],
      command      = command,
      args         = args.toList
    )

  given Show[Cmd[?, ?, ?]] = Show.fromToString
