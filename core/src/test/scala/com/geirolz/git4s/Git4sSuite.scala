package com.geirolz.git4s

import cats.effect.IO
import com.geirolz.git4s.cmd.CmdRunner
import com.geirolz.git4s.data.GitVersion
import com.geirolz.git4s.log.history.CmdHistoryLogger
import com.geirolz.git4s.testing.CmdRunnerStub
import com.geirolz.git4s.testing.CmdRunnerStub.history

class Git4sSuite extends munit.CatsEffectSuite {

  test("test Error") {
    CmdRunnerStub[IO].stderr("git version 1.2.3") {
      for {
        _ <- assertIO(
          Git4s[IO].version,
          GitVersion(1, 2, 3)
        )
//        _ <- assertIO(
//          obtained = history[IO].cmds,
//          returns  = List("git version")
//        )
      } yield ()
    }
  }

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
