package git4s.parser

import cats.effect.IO
import fs2.Chunk
import fs2.io.file.Path
import git4s.data.diff.{CodeBlock, FileDiff}
import git4s.data.diff.FileDiff.NewFile
import git4s.data.parser.FileDiffParser
import git4s.testing.*

class FileDiffParserSuite extends munit.CatsEffectSuite {

  private def parse(s: String): IO[List[FileDiff]] =
    lines(s)
      .through(FileDiffParser[IO].parse)
      .compile
      .toList

  test("New") {
    assertIO(
      obtained = parse(
        s"""diff --git a/baz.md b/baz.md
           |new file mode 100644
           |index 0000000..aa39060
           |--- /dev/null
           |+++ b/newfile.md
           |@@ -0,0 +1,0 @@
           |+newfile
           |""".stripMargin
      ),
      returns = List(
        NewFile(
          Path("baz.md"),
          CodeBlock(
            startLine   = 0,
            startColumn = 0,
            endLine     = 0,
            endColumn   = 0,
            lines       = Chunk("newfile")
          )
        )
      )
    )
  }

  test("Deleted") {
    assertIO(
      obtained = parse(
        s"""diff --git a/deleted.md b/deleted.md
           |deleted file mode 100644
           |index aa39060..0000000
           |--- a/deleted.md
           |+++ /dev/null
           |@@ -1 +0,0 @@
           |-deletedFile
           |""".stripMargin
      ),
      returns = List(
        FileDiff.DeletedFile(
          sourceFile = Path("deleted.md"),
          content = CodeBlock(
            startLine   = 1,
            startColumn = 0,
            endLine     = 1,
            endColumn   = 0,
            lines       = Chunk("deletedFile")
          )
        )
      )
    )
  }

  test("Renamed") {
    assertIO(
      obtained = parse(
        s"""diff --git a/old.md b/new.md
         |similarity index 100%
         |rename from old.md
         |rename to new.md
         |""".stripMargin.stripMargin
      ),
      returns = List(
        FileDiff.RenamedFile(
          from = Path("old.md"),
          to   = Path("new.md")
        )
      )
    )
  }

  def conflictFile(name: String = "README.md"): String =
    s"""diff --cc $name
       |index 2445f65,f4b8569..0000000
       |--- a/$name
       |+++ b/$name
       |@@@ -8,7 -8,7 +8,11 @@@
       |  npm install parse-git-diff
       |
       |
       |++<<<<<<< HEAD
       | +## a
       |++=======
       |+ ## b
       |++>>>>>>> branch-b
       |
       |  - [demo](https://yeonjuan.github.io/parse-git-diff/)
       |""".stripMargin

  def newLineFile(name: String = "README.md"): String =
    s"""diff --git a/$name b/$name
       |index aa39060..0e05564 100644
       |--- a/rename.md
       |+++ b/rename.md
       |@@ -1 +1,2 @@
       | newfile
       |+newline""".stripMargin

  def deletedLineFile(name: String = "README.md"): String =
    s"""diff --git a/$name b/$name
       |index 0e05564..aa39060 100644
       |--- a/rename.md
       |+++ b/rename.md
       |@@ -1,2 +1 @@
       | newfile
       |-newline""".stripMargin
}
