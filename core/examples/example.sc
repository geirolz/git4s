import com.geirolz.git4s.data.diff.FileDiff
import com.geirolz.git4s.{Git4s, Git4sReset}
//given logger: CmdLogger[IO] = CmdLogger.console[IO](LogFilter.all)
//val res = fs2.Stream.emit(
//  """
//      |diff --git a/core/src/main/scala/com/geirolz/git4s/Bar.scala b/core/src/main/scala/com/geirolz/git4s/Bar.scala
//      |new file mode 100644
//      |index 0000000..776d244
//      |--- /dev/null
//      |+++ b/core/src/main/scala/com/geirolz/git4s/Bar.scala
//      |@@ -0,0 +1,5 @@
//      |+package com.geirolz.git4s
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
//      |+package com.geirolz.git4s
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
//git4s.localConfig.unset("tesd").unsafeRunSync()