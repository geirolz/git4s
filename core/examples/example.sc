import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import com.geirolz.git4s.Git4s
import com.geirolz.git4s.data.request.GitConfigKey
import com.geirolz.git4s.log.{CmdLogger, LogFilter}
import fs2.io.file.Path

val path = "/Users/davidgeirola/IdeaProjects/geirolz/cats-git"
val git4s: Git4s[IO] = Git4s[IO].withWorkingDirectory(path)

given logger: CmdLogger[IO] = CmdLogger.console[IO](LogFilter.all)



//git4s.status().unsafeRunSync()
git4s.localConfig.unset("tesd").unsafeRunSync()
