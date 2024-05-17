import cats.effect.kernel.Async
import fs2.Stream
import git4s.cmd.error.CmdFailure

for {
  _ <- Async[F].unit
  //              stdOutTopic <- Topic[F, String]
  //              stdErrTopic <- Topic[F, String]
  stdout = p.stdout
    //                .through(stdOutTopic.publish)
    .through(cmd.decoder.decode)
    .rethrow
  stderr = p.stderr
    //                .through(stdErrTopic.publish)
    .through(cmd.errorDecoder.decode)
    .rethrow
    .flatMap {
      case e: Throwable => Stream.raiseError(e)
      case e: E         => Stream.raiseError(CmdFailure(e.toString))
    }
  //              logStream = Stream
  //                .eval(p.exitValue)
  //                .evalMap(exitCode => CmdLogger[F].log(cmd.compiled, cmd.in, exitCode).tupleLeft(exitCode))

  //                .flatMap {
  //                  case (0, logAction) => stdOutTopic.subscribeUnbounded.evalMap(logAction).drain
  //                  case (_, logAction) => stdErrTopic.subscribeUnbounded.evalMap(logAction).drain
  //                }
} yield stdout.concurrently(stderr)
