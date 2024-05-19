package git4s.testing

import cats.effect.IO
import git4s.Git4s
import git4s.logging.CmdLogger
import munit.CatsEffectAssertions.assertIO

trait CmdIOSuite extends munit.CatsEffectSuite:

  def testCmd[T](name: String)(
    run: CmdLogger[IO] ?=> Git4s[IO] => IO[T],
    stubbedStdout: List[String],
    expectedCmds: List[String],
    expected: T
  ): Unit = test(name) {
    assertStdout(run, stubbedStdout, expectedCmds, expected)
  }

  def assertStdout[T](
    run: CmdLogger[IO] ?=> Git4s[IO] => IO[T],
    stubbedStdout: List[String],
    expectedCmds: List[String],
    expected: T
  ): IO[Unit] =
    CmdRunnerStub[IO].stdout(fs2.Stream.emits(stubbedStdout)) {
      for {
        _ <- assertIO(
          obtained = run(Git4s[IO]),
          returns  = expected
        )
        _ <- assertIO(
          obtained = CmdRunnerStub.history[IO].cmds,
          returns  = expectedCmds
        )
      } yield ()
    }
