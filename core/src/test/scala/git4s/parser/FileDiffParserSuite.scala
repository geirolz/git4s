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

//  test("Parse diff with changes files") {
//
//    val res: IO[List[FileDiff]] = lines(
//      aDummyLine,
//      aDummyLine,
//      modifiedFile("test/Foo.txt"),
//      aDummyLine,
////      modifiedFile("test/Bar.txt"),
////      aDummyLine,
////      aDummyLine
//    )
//      .through(FileDiffParser[IO].parse)
//      .compile
//      .toList
//
//    assertIO(
//      obtained = res,
//      returns = List(
//        FileDiff.ModifiedFile(
//          Path("test/Foo.txt"),
//          LazyList(
//            CodeBlock.empty -> CodeBlock.empty
//          )
//        )
//      )
//    )
//  }

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

  def modifiedFile(name: String): String =
    s"""
       |diff --git a/$name.scala b/$name.scala
       |index 776d244..0000000
       |--- a/$name.scala
       |+++ b/$name.scala
       |@@ -31,7 +31,7 @@ libraryDependencies += "com.github.geirolz" %% "git4s" % "<version>"
       | - [x] git config [local | global]
       |
       | **tag**
       |-- [ ] git tag
       |+- [x] git tag [list | create | delete | replace]
       |
       | **repository**
       | - [x] git status
       |
       |""".stripMargin

}
