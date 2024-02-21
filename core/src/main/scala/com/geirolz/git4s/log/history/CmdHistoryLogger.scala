package com.geirolz.git4s.log.history

import cats.effect.kernel.{Async, Ref}
import cats.effect.std.Console
import cats.syntax.all.*
import com.geirolz.git4s.cmd.{currentWorkingDir, WorkingCtx}
import com.geirolz.git4s.log.{CmdLogger, LogFilter, LogFormatter}
import fs2.Stream

class CmdHistoryLogger[F[_]: Async](
  history: Ref[F, List[CmdHistoryLogger.Log]],
  filter: LogFilter
)(using
  formatter: LogFormatter
) extends CmdLogger[F]:

  def get: F[List[CmdHistoryLogger.Log]] = history.get
  def cmds: F[List[String]]              = history.get.map(_.map(_.cmd))
  def clear: F[Unit]                     = history.set(List.empty)

  override def log(
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
      .flatMap(s => history.update(_ :+ CmdHistoryLogger.Log(compiledCmd, exitCode, s)))
      .whenA(filter(exitCode))
      .as(str =>
        history
          .update(xs => {
            val last = xs.last
            xs.updated(xs.size - 1, last.append(str))
          })
          .whenA(filter(exitCode))
      )

object CmdHistoryLogger:

  def apply[F[_]: Async: Console](filter: LogFilter = LogFilter.all)(using
    formatter: LogFormatter
  ): F[CmdHistoryLogger[F]] =
    Ref.of[F, List[Log]](List.empty).map(new CmdHistoryLogger(_, filter))

  case class Log(cmd: String, exitCode: Int, output: String):
    def append(str: String): CmdHistoryLogger.Log =
      copy(output = output + str)
