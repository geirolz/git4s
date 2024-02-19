package com.geirolz.git4s.cmd

import cats.{Functor, Show}
import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.CmdError
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.log.CmdLogger
import fs2.Stream
import fs2.io.file.Path

private[git4s] final case class Cmd[F[_]: Async, E, T](
  decoder: CmdDecoder[F, T],
  errorDecoder: CmdDecoder[F, E],
  command: String,
  args: List[String]    = List.empty,
  in: Stream[F, String] = Stream.empty
):

  inline def setArgs(args: String*): Cmd[F, E, T] =
    copy(args = args.toList.filter(_.nonEmpty))

  inline def addArgs(args: String*): Cmd[F, E, T] =
    setArgs(this.args ++ args*)

  inline def addOptArgs(args: Option[String]*): Cmd[F, E, T] =
    addArgs(args.flatten.toList*)

  inline def addFlagArgs(flags: (Boolean, String)*): Cmd[F, E, T] =
    addArgs(flags.collect { case (true, flag) => flag }*)

  inline def withInput(input: Stream[F, String]): Cmd[F, E, T] =
    copy(in = input)

  /** This may not be the actual command used by the runner */
  inline def compiled: String =
    (command :: args).mkString(" ")

  override def toString: String = compiled

  // ---------------- Runners ----------------
  def runAsStream(using WorkingCtx, CmdRunner[F], CmdLogger[F]): Stream[F, T] =
    CmdRunner[F].stream(this)

  def runGetLast(using WorkingCtx, CmdRunner[F], CmdLogger[F]): F[T] =
    CmdRunner[F].last(this)

  def run_(using WorkingCtx, CmdRunner[F], Functor[F], CmdLogger[F]): F[Unit] =
    runGetLast.void

private[git4s] object Cmd:

  def simple_[F[_]: Async](command: String, args: String*): Cmd[F, CmdError, Unit] =
    apply[F, CmdError, Unit](command, args: _*)

  def simple[F[_]: Async](command: String, args: String*): Cmd[F, CmdError, String] =
    apply[F, CmdError, String](command, args: _*)

  def apply[F[_]: Async, E: CmdDecoder[F, *], T: CmdDecoder[F, *]](
    command: String,
    args: String*
  ): Cmd[F, E, T] =
    new Cmd(
      decoder      = CmdDecoder[F, T],
      errorDecoder = CmdDecoder[F, E],
      command      = command,
      args         = args.toList
    )

  given Show[Cmd[?, ?, ?]] = Show.fromToString
