package git4s

import cats.effect.IO
import git4s.testing.{CmdIOSuite, CmdRunnerStub}
import git4s.testing.CmdRunnerStub.history
import git4s.Git4s
import git4s.cmd.CmdRunner
import git4s.data.GitVersion
import git4s.logging.history.CmdHistoryLogger

class Git4sSuite extends CmdIOSuite:

  testCmd("Git4s.version")(
    run           = _.version,
    stubbedStdout = List("git version 1.2.3"),
    expectedCmds  = List("git version"),
    expected      = GitVersion(1, 2, 3)
  )

  testCmd("Git4s.isInstalled")(
    run           = _.isInstalled,
    stubbedStdout = List("git version 1.2.3"),
    expectedCmds  = List("git version"),
    expected      = true
  )
