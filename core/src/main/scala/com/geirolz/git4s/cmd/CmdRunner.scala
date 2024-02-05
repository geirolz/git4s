package com.geirolz.git4s.cmd

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.cmd.error.{CmdError, CmdFailure}
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.log.CmdLogger
import fs2.io.file.Path
import fs2.io.process.*

private[git4s] trait CmdRunner[F[_]]:
  def run_(command: String, args: String*)(using WorkingCtx, CmdLogger[F]): F[Unit]
  def run[E: CmdDecoder, T: CmdDecoder](command: String, args: String*)(using WorkingCtx, CmdLogger[F]): F[T]
  def run[E, T](pd: Cmd[F, E, T])(using WorkingCtx, CmdLogger[F]): F[T]

private[git4s] object CmdRunner:
  transparent inline def apply[F[_]](using p: CmdRunner[F]): CmdRunner[F] = p
  given [F[_]: Processes](using F: Async[F]): CmdRunner[F] = new CmdRunner[F]:

    override def run_(command: String, args: String*)(using WorkingCtx, CmdLogger[F]): F[Unit] =
      run(Cmd[F, CmdError, Unit](command, args*))

    override def run[E: CmdDecoder, T: CmdDecoder](
      command: String,
      args: String*
    )(using WorkingCtx, CmdLogger[F]): F[T] =
      run(Cmd[F, E, T](command, args*))

    override def run[E, T](pd: Cmd[F, E, T])(using WorkingCtx, CmdLogger[F]): F[T] =

      def applyWorkingDir(pb: ProcessBuilder): ProcessBuilder =
        currentWorkingDir.fold(pb.withCurrentWorkingDirectory)(pb.withWorkingDirectory)

      applyWorkingDir(ProcessBuilder(pd.command, pd.args*))
        .spawn[F]
        .map(CmdProcess.fromProcess[F](_, pd.in))
        .use(p =>
          p.exitValue.flatMap {
            case 0 =>
              p.stdout.compile.string
                .flatTap(CmdLogger[F].log(pd, 0, _))
                .map(pd.decoder.decode)
                .rethrow

            case errorCode =>
              p.stderr.compile.string
                .flatTap(CmdLogger[F].log(pd, 0, _))
                .map(pd.errorDecoder.decode)
                .rethrow
                .flatMap {
                  case e: Throwable => F.raiseError(e)
                  case e: E         => F.raiseError(CmdFailure(e.toString))
                }
          }
        )
