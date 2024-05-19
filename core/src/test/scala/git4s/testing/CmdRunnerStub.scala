package git4s.testing

import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Console
import cats.syntax.all.*
import git4s.cmd.{Cmd, CmdProcess, CmdRunner}
import git4s.logging.history.CmdHistoryLogger

class CmdRunnerStub[F[_]: Async: Console]:
  
  def stdout[T](s1: String, sN: String*)(f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]): F[T] =
    stdout(fs2.Stream.emits(s1 +: sN))(f)

  def stdout[T](str: fs2.Stream[F, String])(f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]): F[T] =
    agnostic(_ =>
      CmdProcess[F](
        _isAlive   = false.pure[F],
        _exitValue = 0.pure[F],
        _stdin     = _.drain,
        _stdout    = str.through(fs2.text.utf8.encode),
        _stderr    = fs2.Stream.empty
      )
    )(f)

  def stderr[T](s1: String, sN: String*)(f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]): F[T] =
    stderr(-1, s1, sN: _*)(f)

  def stderr[T](exitCode: Int, s1: String, sN: String*)(f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]): F[T] =
    stderr(exitCode, fs2.Stream.emits(s1 +: sN))(f)

  def stderr[T](str: fs2.Stream[F, String])(f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]): F[T] =
    stderr(-1, str)(f)

  def stderr[T](exitCode: Int, str: fs2.Stream[F, String])(
    f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]
  ): F[T] =
    agnostic(_ =>
      CmdProcess[F](
        _isAlive   = false.pure[F],
        _exitValue = exitCode.pure[F],
        _stdin     = _.drain,
        _stdout    = fs2.Stream.empty,
        _stderr    = str.through(fs2.text.utf8.encode)
      )
    )(f)

  def agnostic[T](cmdProcess: Cmd[F, ?, ?] => CmdProcess[F])(f: (CmdRunner[F], CmdHistoryLogger[F]) ?=> F[T]): F[T] =
    val runner: CmdRunner[F] = CmdRunner.fromCmdProcess[F](cmd => Resource.pure[F, CmdProcess[F]](cmdProcess(cmd)))
    CmdHistoryLogger[F]().flatMap(logger => {
      f(using runner, logger)
    })

object CmdRunnerStub:
  def history[F[_]](using h: CmdHistoryLogger[F]): CmdHistoryLogger[F] = h
