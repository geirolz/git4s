import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import fs2.io.file.Path
import git4s.Git4s
import git4s.data.value.*
import git4s.logging.*

val path             = "/Users/davidgeirola/IdeaProjects/geirolz/cats-git"
val git4s: Git4s[IO] = Git4s[IO].withWorkingDirectory(path)

given logger: CmdLogger[IO] = CmdLogger.console[IO](LogFilter.all)

//val res = fs2.Stream.emit(
//  """
//      |diff --git a/core/src/main/scala/com/geirolz/git4s/Bar.scala b/core/src/main/scala/com/geirolz/git4s/Bar.scala
//      |new file mode 100644
//      |index 0000000..776d244
//      |--- /dev/null
//      |+++ b/core/src/main/scala/com/geirolz/git4s/Bar.scala
//      |@@ -0,0 +1,5 @@
//      |+package git4s
//      |+
//      |+class Bar {
//      |+
//      |+}
//      |
//      |diff --git a/core/src/main/scala/com/geirolz/git4s/Foo.scala b/core/src/main/scala/com/geirolz/git4s/Foo.scala
//      |new file mode 100644
//      |index 0000000..776d244
//      |--- /dev/null
//      |+++ b/core/src/main/scala/com/geirolz/git4s/Foo.scala
//      |@@ -0,0 +1,5 @@
//      |+package git4s
//      |+
//      |+class Foo {
//      |+
//      |+}
//      |
//      |""".stripMargin
//  )
//  .through(fs2.text.lines)
//  .through(FileDiff.given_CmdDecoder_F_FileDiff[IO].decode)
//  .compile
//  .toList
//  .unsafeRunSync()

//git4s.diff(Some("main")).compile.toList.unsafeRunSync()

//res.size
//git4s.uninstall
//git4s.status.unsafeRunSync()
//git4s.localConfig.unset("tesd").unsafeRunSync()

//git4s.log.compile.toList.unsafeRunSync()

git4s.log().compile.toList.unsafeRunSync()

//git4s.tag.list().compile.toList.unsafeRunSync()
//git4s.tag.exists(CommitTag("v0.0.2")).unsafeRunSync()
