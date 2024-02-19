package com.geirolz.git4s.data.diff

import cats.effect.IO
import com.geirolz.git4s.data.diff.FileDiff

class FileDiffSuite extends munit.CatsEffectSuite {

  test("Parse diff with new files") {

    val res: IO[List[FileDiff]] = fs2.Stream
      .emit(
        s"""
           |IGNORE THIS LINE
           |${newFile("Baz")}
           |IGNORE THIS LINE
           |${newFile("Bar")}
           |IGNORE THIS LINE
           |""".stripMargin
      )
      .through(FileDiff.given_CmdDecoder_F_FileDiff[IO].decode)
      .rethrow
      .compile
      .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 2
    )
  }

  test("Parse diff with deleted files") {

    val res: IO[List[FileDiff]] = fs2.Stream
      .emit(
        s"""
          |IGNORE THIS LINE
          |${deletedFile("Baz")}
          |IGNORE THIS LINE
          |${deletedFile("Bar")}
          |IGNORE THIS LINE
          |""".stripMargin
      )
      .through(FileDiff.given_CmdDecoder_F_FileDiff[IO].decode)
      .rethrow
      .compile
      .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 2
    )
  }

  test("Parse diff with renamed files") {

    val res: IO[List[FileDiff]] = fs2.Stream
      .emit(
        s"""
           |IGNORE THIS LINE
           |${renamedFile("test/Foo.txt", "test/Bar.txt")}
           |IGNORE THIS LINE
           |${renamedFile("test/Bar.txt", "test/A/Baz.txt")}
           |IGNORE THIS LINE
           |""".stripMargin
      )
      .through(FileDiff.given_CmdDecoder_F_FileDiff[IO].decode)
      .rethrow
      .compile
      .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 2
    )
  }

  def newFile(name: String): String =
    s"""
      |diff --git a/$name.scala b/$name.scala
      |new file mode 100644
      |index 0000000..776d244
      |--- /dev/null
      |+++ b/$name.scala
      |@@ -0,0 +1,5 @@
      |+package com.geirolz.git4s
      |+
      |+class $name {
      |+
      |+}
      |""".stripMargin

  def deletedFile(name: String): String =
    s"""
      |diff --git a/$name.scala b/$name.scala
      |deleted file mode 100644
      |index 776d244..0000000
      |--- a/$name.scala
      |+++ /dev/null
      |@@ -1,5 +0,0 @@
      |-package com.geirolz.git4s
      |-
      |-class Bar {
      |-
      |-}
      |""".stripMargin

  def renamedFile(from: String, to: String): String =
    s"""diff --git a/$from b/$to
      |similarity index 100%
      |rename from $from
      |rename to $to
      |""".stripMargin
}
