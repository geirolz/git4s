package com.geirolz.git4s.log

import cats.effect.Resource
import cats.effect.kernel.Clock
import cats.effect.std.Console
import cats.syntax.all.*
import cats.{Applicative, Monad}
import com.geirolz.git4s.cmd.{currentWorkingDir, Cmd, WorkingCtx}
import fs2.Chunk
import fs2.io.file.{Files, Flags, Path}

import java.time.LocalDateTime

trait CmdLogger[F[_]]:

  // TODO do not use Cmd here, create CmdInfo or something like that
  def log[E, T](
    cmd: Cmd[F, E, T],
    exitCode: Int,
    result: String
  )(using WorkingCtx): F[Unit]

object CmdLogger:

  def apply[F[_]](using logger: CmdLogger[F]): CmdLogger[F] = logger

  def console[F[_]: Applicative: Console](
    filter: LogFilter = LogFilter.onlyFailures
  )(using formatter: LogFormatter): CmdLogger[F] = new CmdLogger[F]:
    def log[E, T](
      cmd: Cmd[F, E, T],
      exitCode: Int,
      result: String
    )(using WorkingCtx): F[Unit] =
      Console[F]
        .println(formatter.format(cmd, currentWorkingDir, exitCode, result))
        .whenA(filter(exitCode))

  def file[F[_]: Monad: Files: Clock](
    path: Path,
    filter: LogFilter = LogFilter.onlyFailures
  )(using formatter: LogFormatter): Resource[F, CmdLogger[F]] =
    Files[F]
      .writeCursor(path, Flags.Append)
      .map(c => {
        new CmdLogger[F]:
          def log[E, T](
            cmd: Cmd[F, E, T],
            exitCode: Int,
            result: String
          )(using WorkingCtx): F[Unit] =
            if (filter(exitCode))
              c.write(
                Chunk.from(
                  formatter
                    .format(
                      cmd        = cmd,
                      workingDir = currentWorkingDir,
                      exitCode   = exitCode,
                      result     = result,
                      datetime   = Some(LocalDateTime.now)
                    )
                    .getBytes
                )
              ).void
            else Monad[F].unit
      })

  given noop[F[_]: Applicative]: CmdLogger[F] with
    def log[E, T](
      cmd: Cmd[F, E, T],
      exitCode: Int,
      result: String
    )(using WorkingCtx): F[Unit] =
      Applicative[F].unit
