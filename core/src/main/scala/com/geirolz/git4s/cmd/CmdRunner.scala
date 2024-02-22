package com.geirolz.git4s.cmd

import cats.effect.kernel.{Async, Resource}
import cats.effect.std.Queue
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.CmdFailure
import com.geirolz.git4s.log.CmdLogger
import fs2.io.file.Path
import fs2.io.process.*
import fs2.{text, Stream}

trait CmdRunner[F[_]]:
  def stream[E, T](pd: Cmd[F, E, T])(using WorkingCtx, CmdLogger[F]): Stream[F, T]
  final inline def last[E, T](pd: Cmd[F, E, T])(using Async[F], WorkingCtx, CmdLogger[F]): F[T] =
    stream(pd).compile.lastOrError

private[git4s] object CmdRunner:
  transparent inline def apply[F[_]](using p: CmdRunner[F]): CmdRunner[F] = p

  def fromCmdProcess[F[_]: Async](fp: WorkingCtx ?=> Cmd[F, ?, ?] => Resource[F, CmdProcess[F]]): CmdRunner[F] =
    new CmdRunner[F]:

      override def stream[E, T](cmd: Cmd[F, E, T])(using WorkingCtx, CmdLogger[F]): Stream[F, T] =
        for {
          process  <- Stream.resource(fp(cmd))
          outTopic <- Stream.eval(Queue.unbounded[F, Option[String]])
          errTopic <- Stream.eval(Queue.unbounded[F, Option[String]])
          out: Stream[F, T] =
            process.stdout
              .through(text.utf8.decode)
              .evalTap(e => outTopic.offer(Some(e)))
              .concurrently(
                cmd.in
                  .through(text.utf8.encode)
                  .through(process.stdin)
              )
              .through(cmd.decoder.decode)
              .rethrow
              .onFinalize(outTopic.offer(None))

          err: Stream[F, Nothing] =
            process.stderr
              .through(text.utf8.decode)
              .evalTap(e => errTopic.offer(Some(e)))
              .through(cmd.errorDecoder.decode)
              .flatMap {
                case Left(e)             => Stream.raiseError(e)
                case Right(e: Throwable) => Stream.raiseError(e)
                case Right(e: E)         => Stream.raiseError(CmdFailure(e.toString))
              }
              .onFinalize(errTopic.offer(None))

          log: Stream[F, Nothing] =
            Stream
              .eval(process.exitValue)
              .evalMap(exitCode => CmdLogger[F].log(cmd.compiled, cmd.in, exitCode).tupleLeft(exitCode))
              .map {
                case (0, logAction) => (outTopic, logAction)
                case (_, logAction) => (errTopic, logAction)
              }
              .flatMap { case (queue, logAction) =>
                Stream
                  .fromQueueNoneTerminated(queue)
                  .evalMap(logAction)
                  .drain
              }

          result <- out.merge(err).merge(log)
        } yield result

  given [F[_]: Processes](using F: Async[F]): CmdRunner[F] =
    CmdRunner.fromCmdProcess((cmd: Cmd[F, ?, ?]) => {

      def applyWorkingDir(pb: ProcessBuilder): ProcessBuilder =
        currentWorkingDir.fold(pb.withCurrentWorkingDirectory)(pb.withWorkingDirectory)

      applyWorkingDir(ProcessBuilder(cmd.command, cmd.args*))
        .spawn[F]
        .map(CmdProcess.fromProcess[F](_))
    })
