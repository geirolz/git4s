package com.geirolz.git4s.cmd

import cats.effect.kernel.{Async, Resource}
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.CmdFailure
import com.geirolz.git4s.log.CmdLogger
import fs2.Stream
import fs2.io.file.Path
import fs2.io.process.*

trait CmdRunner[F[_]]:
  def stream[E, T](pd: Cmd[F, E, T])(using WorkingCtx, CmdLogger[F]): Stream[F, T]
  final inline def last[E, T](pd: Cmd[F, E, T])(using Async[F], WorkingCtx, CmdLogger[F]): F[T] =
    stream(pd).compile.lastOrError

private[git4s] object CmdRunner:
  transparent inline def apply[F[_]](using p: CmdRunner[F]): CmdRunner[F] = p

  def fromCmdProcess[F[_]: Async](fp: WorkingCtx ?=> Cmd[F, ?, ?] => Resource[F, CmdProcess[F]]): CmdRunner[F] =
    new CmdRunner[F]:
      override def stream[E, T](cmd: Cmd[F, E, T])(using WorkingCtx, CmdLogger[F]): Stream[F, T] =
        Stream
          .eval(
            fp(cmd).use((p: CmdProcess[F]) =>
              p.exitValue.flatMap {
                case 0 =>
                  CmdLogger[F]
                    .log(cmd.compiled, cmd.in, 0)
                    .map(logAction => {
                      p.stdout
                        .evalTap(logAction)
                        .through(cmd.decoder.decode)
                        .rethrow
                    })
                case errorCode =>
                  CmdLogger[F]
                    .log(cmd.compiled, cmd.in, errorCode)
                    .map(logAction => {
                      p.stderr
                        .evalTap(logAction)
                        .through(cmd.errorDecoder.decode)
                        .rethrow
                        .flatMap {
                          case e: Throwable => Stream.raiseError(e)
                          case e: E         => Stream.raiseError(CmdFailure(e.toString))
                        }
                    })
              }
            )
          )
          .flatten

  given [F[_]: Processes](using F: Async[F]): CmdRunner[F] =
    CmdRunner.fromCmdProcess((cmd: Cmd[F, ?, ?]) => {

      def applyWorkingDir(pb: ProcessBuilder): ProcessBuilder =
        currentWorkingDir.fold(pb.withCurrentWorkingDirectory)(pb.withWorkingDirectory)

      applyWorkingDir(ProcessBuilder(cmd.command, cmd.args*))
        .spawn[F]
        .map(CmdProcess.fromProcess[F](_, cmd.in))
    })
