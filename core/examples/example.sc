given logger: CmdLogger[IO] = CmdLogger.console[IO](LogFilter.all)
git4s.status.unsafeRunSync()