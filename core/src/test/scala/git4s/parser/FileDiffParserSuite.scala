package git4s.parser

import cats.effect.IO
import git4s.data.diff.FileDiff
import git4s.data.parser.FileDiffParser
import git4s.testing.*

class FileDiffParserSuite extends munit.CatsEffectSuite {

  test("Parse diff with new files") {

    val res: IO[List[FileDiff]] =
      lines(
        s"""
           |$aDummyLine
           |${newFile("Baz")}
           |$aDummyLine
           |${newFile("Bar")}
           |$aDummyLine
           |""".stripMargin
      )
        .through(FileDiffParser[IO].parse)
        .compile
        .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 2
    )
  }

  test("Parse diff with deleted files") {

    val res: IO[List[FileDiff]] =
      lines(
        s"""
          |$aDummyLine
          |${deletedFile("Baz")}
          |$aDummyLine
          |${deletedFile("Bar")}
          |$aDummyLine
          |""".stripMargin
      )
        .through(FileDiffParser[IO].parse)
        .compile
        .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 2
    )
  }

  test("Parse diff with renamed files") {

    val res: IO[List[FileDiff]] = lines(
      s"""
           |$aDummyLine
           |${renamedFile("test/Foo.txt", "test/Bar.txt")}
           |$aDummyLine
           |${renamedFile("test/Bar.txt", "test/A/Baz.txt")}
           |$aDummyLine
           |""".stripMargin
    )
      .through(FileDiffParser[IO].parse)
      .compile
      .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 2
    )
  }

  test("Parse diff with changed files") {

    val res: IO[List[FileDiff]] = lines(
      s"""
         |$aDummyLine
         |${renamedFile("test/Foo.txt", "test/Bar.txt")}
         |$aDummyLine
         |${renamedFile("test/Bar.txt", "test/A/Baz.txt")}
         |IGNORE THIS LINE
         |""".stripMargin
    )
      .through(FileDiffParser[IO].parse)
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
      |+package git4s
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
      |-package git4s
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
