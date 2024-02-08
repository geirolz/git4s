package com.geirolz.git4s.log

import cats.effect.Resource
import cats.effect.kernel.{Clock, Concurrent}
import cats.effect.std.Console
import cats.syntax.all.*
import cats.{Applicative, Monad}
import com.geirolz.git4s.cmd.{currentWorkingDir, WorkingCtx}
import fs2.io.file.{Files, Flags, Path}
import fs2.{Chunk, Stream}

import java.time.LocalDateTime

trait CmdLogger[F[_]]:

  def log(
    compiledCmd: String,
    in: Stream[F, String],
    exitCode: Int,
    result: String
  )(using WorkingCtx): F[Unit]

object CmdLogger:

  def apply[F[_]](using logger: CmdLogger[F]): CmdLogger[F] = logger

  def console[F[_]: Concurrent: Console](
    filter: LogFilter = LogFilter.onlyFailures
  )(using formatter: LogFormatter): CmdLogger[F] = new CmdLogger[F]:
    def log(
      compiledCmd: String,
      in: Stream[F, String],
      exitCode: Int,
      result: String
    )(using WorkingCtx): F[Unit] =
      formatter
        .format[F](
          compiledCmd = compiledCmd,
          in          = in,
          workingDir  = currentWorkingDir,
          exitCode    = exitCode,
          result      = result,
          datetime    = None,
          verbose     = WorkingCtx.current.verboseLog
        )
        .flatMap(Console[F].println(_))
        .whenA(filter(exitCode))

  def file[F[_]: Concurrent: Files: Clock](
    path: Path,
    filter: LogFilter = LogFilter.onlyFailures
  )(using formatter: LogFormatter): Resource[F, CmdLogger[F]] =
    Files[F]
      .writeCursor(path, Flags.Append)
      .map(c => {
        new CmdLogger[F]:
          def log(
            compiledCmd: String,
            in: Stream[F, String],
            exitCode: Int,
            result: String
          )(using WorkingCtx): F[Unit] =
            if (filter(exitCode))
              formatter
                .format[F](
                  compiledCmd = compiledCmd,
                  in          = in,
                  workingDir  = currentWorkingDir,
                  exitCode    = exitCode,
                  result      = result,
                  datetime    = Some(LocalDateTime.now),
                  verbose     = WorkingCtx.current.verboseLog
                )
                .map(s => Chunk.from(s.getBytes))
                .flatMap(c.write(_))
                .void
            else Monad[F].unit
      })

  given noop[F[_]: Applicative]: CmdLogger[F] with
    def log(
      compiledCmd: String,
      in: Stream[F, String],
      exitCode: Int,
      result: String
    )(using WorkingCtx): F[Unit] =
      Applicative[F].unit
