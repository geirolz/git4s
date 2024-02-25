package com.geirolz.git4s.log

import cats.Applicative
import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.syntax.all.*
import com.geirolz.git4s.cmd.{currentWorkingDir, WorkingCtx}
import com.geirolz.git4s.log.history.CmdHistoryLogger
import fs2.Stream

trait CmdLogger[F[_]]:

  def log(
    compiledCmd: String,
    in: Stream[F, String],
    exitCode: Int
  )(using WorkingCtx): F[String => F[Unit]]

object CmdLogger:

  def apply[F[_]](using logger: CmdLogger[F]): CmdLogger[F] = logger

  export CmdHistoryLogger.apply as history

  def console[F[_]: Async: Console](
    filter: LogFilter = LogFilter.onlyFailures
  )(using formatter: LogFormatter): CmdLogger[F] = new CmdLogger[F]:
    def log(
      compiledCmd: String,
      in: Stream[F, String],
      exitCode: Int
    )(using WorkingCtx): F[String => F[Unit]] =
      formatter
        .format[F](
          compiledCmd = compiledCmd,
          in          = in,
          workingDir  = currentWorkingDir,
          exitCode    = exitCode,
          datetime    = None,
          verbose     = WorkingCtx.current.verboseLog
        )
        .flatMap(Console[F].println(_))
        .whenA(filter(exitCode))
        .as(Console[F].println(_).whenA(filter(exitCode)))

//  def file[F[_]: Concurrent: Files: Clock](
//    path: Path,
//    filter: LogFilter = LogFilter.onlyFailures
//  )(using formatter: LogFormatter): Resource[F, CmdLogger[F]] =
//    Files[F]
//      .writeCursor(path, Flags.Append)
//      .map(c => {
//        new CmdLogger[F]:
//          def log(
//            compiledCmd: String,
//            in: Stream[F, String],
//            exitCode: Int,
//            result: String
//          )(using WorkingCtx): F[Unit] =
//            if (filter(exitCode))
//              formatter
//                .format[F](
//                  compiledCmd = compiledCmd,
//                  in          = in,
//                  workingDir  = currentWorkingDir,
//                  exitCode    = exitCode,
//                  result      = result,
//                  datetime    = Some(LocalDateTime.now),
//                  verbose     = WorkingCtx.current.verboseLog
//                )
//                .map(s => Chunk.from(s.getBytes))
//                .flatMap(c.write(_))
//                .void
//            else Monad[F].unit
//      })

  given noop[F[_]: Applicative]: CmdLogger[F] with
    def log(
      compiledCmd: String,
      in: Stream[F, String],
      exitCode: Int
    )(using WorkingCtx): F[String => F[Unit]] =
      Applicative[F].pure(_ => Applicative[F].unit)
