package git4s

import cats.effect.IO
import git4s.testing.CmdRunnerStub
import git4s.testing.CmdRunnerStub.history
import git4s.Git4s
import git4s.cmd.CmdRunner
import git4s.data.GitVersion
import git4s.logging.history.CmdHistoryLogger

class Git4sSuite extends munit.CatsEffectSuite {

  test("Git4s.version") {
    CmdRunnerStub[IO].stdout("git version 1.2.3") {
      for {
        _ <- assertIO(
          Git4s[IO].version,
          GitVersion(1, 2, 3)
        )
        _ <- assertIO(
          obtained = history[IO].cmds,
          returns  = List("git version")
        )
      } yield ()

    }
  }

  test("Git4s.isInstalled") {
    CmdRunnerStub[IO].stdout("git version 1.2.3") {
      for {
        _ <- assertIO(
          Git4s[IO].isInstalled,
          true
        )
        _ <- assertIO(
          obtained = history[IO].cmds,
          returns  = List("git version")
        )
      } yield ()
    }
  }
}
