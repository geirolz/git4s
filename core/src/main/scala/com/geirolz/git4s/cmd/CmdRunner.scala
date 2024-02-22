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
        Stream
          .resource(fp(cmd))
          .evalMap((p: CmdProcess[F]) => {
            for {
              _        <- Async[F].unit
              outTopic <- Queue.unbounded[F, String]
              errTopic <- Queue.unbounded[F, String]

              in: Stream[F, Nothing] =
                cmd.in
                  .through(text.utf8.encode)
                  .through(p.stdin)

              out: Stream[F, T] =
                p.stdout
                  .through(text.utf8.decode)
                  .concurrently(in)
                  .evalTap(outTopic.tryOffer(_).void)
                  .debug()
                  .through(cmd.decoder.decode)
                  .rethrow

              err: Stream[F, Nothing] =
                p.stderr
                  .through(text.utf8.decode)
                  .evalTap(errTopic.tryOffer(_).void)
                  .through(cmd.errorDecoder.decode)
                  .flatMap {
                    case Left(e)             => Stream.raiseError(e)
                    case Right(e: Throwable) => Stream.raiseError(e)
                    case Right(e: E)         => Stream.raiseError(CmdFailure(e.toString))
                  }

              logStream: Stream[F, Nothing] =
                Stream
                  .eval(p.exitValue)
                  .evalMap(exitCode => CmdLogger[F].log(cmd.compiled, cmd.in, exitCode).tupleLeft(exitCode))
                  .map {
                    case (0, logAction) => (outTopic, logAction)
                    case (_, logAction) => (errTopic, logAction)
                  }
                  .flatMap { case (queue, logAction) =>
                    Stream
                      .eval(queue.tryTake)
                      .repeat
                      .unNoneTerminate
                      .evalMap(logAction)
                      .drain
                  }
            } yield out.merge(err).merge(logStream)
          })
          .flatten

  given [F[_]: Processes](using F: Async[F]): CmdRunner[F] =
    CmdRunner.fromCmdProcess((cmd: Cmd[F, ?, ?]) => {

      def applyWorkingDir(pb: ProcessBuilder): ProcessBuilder =
        currentWorkingDir.fold(pb.withCurrentWorkingDirectory)(pb.withWorkingDirectory)

      applyWorkingDir(ProcessBuilder(cmd.command, cmd.args*))
        .spawn[F]
        .map(CmdProcess.fromProcess[F](_))
    })
