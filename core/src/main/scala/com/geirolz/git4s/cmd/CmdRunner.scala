package com.geirolz.git4s.cmd

import cats.effect.kernel.{Async, Resource}
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.CmdFailure
import com.geirolz.git4s.log.CmdLogger
import fs2.Stream
import fs2.concurrent.Topic
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
          .resource(fp(cmd))
          .evalMap((p: CmdProcess[F]) => {
            for {
              _        <- Async[F].unit
              outTopic <- Topic[F, String]
              errTopic <- Topic[F, String]
              out: Stream[F, T] = p.stdout
                .evalTap(outTopic.publish1(_))
                .through(cmd.decoder.decode)
                .rethrow
                .evalTap(_ => outTopic.close.void)
              err: Stream[F, Any] =
                p.stderr
                  .evalTap(errTopic.publish1(_))
                  .through(cmd.errorDecoder.decode)
                  .rethrow
                  .flatMap {
                    case e: Throwable => Stream.raiseError(e)
                    case e: E         => Stream.raiseError(CmdFailure(e.toString))
                  }
                  .evalTap(_ => errTopic.close.void)
                  .ifEmpty(Stream.eval(errTopic.close.void))

              logStream = Stream
                .eval(p.exitValue)
                .evalMap(exitCode => CmdLogger[F].log(cmd.compiled, cmd.in, exitCode).tupleLeft(exitCode))
                .flatMap {
                  case (0, logAction) => outTopic.subscribeUnbounded.evalMap(logAction).drain
                  case (_, logAction) => errTopic.subscribeUnbounded.evalMap(logAction).drain
                }
            } yield out.merge(logStream).concurrently(err)
          })
          .flatten

  given [F[_]: Processes](using F: Async[F]): CmdRunner[F] =
    CmdRunner.fromCmdProcess((cmd: Cmd[F, ?, ?]) => {

      def applyWorkingDir(pb: ProcessBuilder): ProcessBuilder =
        currentWorkingDir.fold(pb.withCurrentWorkingDirectory)(pb.withWorkingDirectory)

      applyWorkingDir(ProcessBuilder(cmd.command, cmd.args*))
        .spawn[F]
        .map(CmdProcess.fromProcess[F](_, cmd.in))
    })
