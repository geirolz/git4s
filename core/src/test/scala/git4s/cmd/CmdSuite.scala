package git4s.cmd

import cats.effect.IO
import git4s.cmd.Cmd
import git4s.data.value.CmdArg.cmd

class CmdSuite extends munit.FunSuite {

  test("Cmd args syntax should be correct") {
    val args: List[String] = Cmd
      .simple[IO](cmd"echo", "hello")
      .addOptArgs(
        Some("-a1"),
        Some("-a2"),
        None,
        Some("-a4")
      )
      .addFlagArgs(
        true  -> "-f1",
        false -> "-f2",
        true  -> "-f3",
        false -> "-f4"
      )
      .addArgs("arg1", "arg2")
      .args

    assertEquals(
      args,
      List(
        "hello",
        "-a1",
        "-a2",
        "-a4",
        "-f1",
        "-f3",
        "arg1",
        "arg2"
      )
    )
  }
}
