import git4s.testing.*
           |$aDummyLine
           |$aDummyLine
           |$aDummyLine
          |$aDummyLine
          |$aDummyLine
          |$aDummyLine
           |$aDummyLine
           |$aDummyLine
           |$aDummyLine
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
