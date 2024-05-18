package git4s.parser

import cats.effect.IO
import git4s.data.GitCommitLog
import git4s.data.parser.CommitLogParser
import git4s.data.value.{CommitAuthorId, CommitDate, CommitId, CommitMessage}
import git4s.testing.*

class GitCommitLogParserSuite extends munit.CatsEffectSuite {

  test("Parse git commit logs") {

    val res: IO[List[GitCommitLog]] =
      lines(
        anEmptyLine,
        anEmptyLine,
        aSimpleCommitLog(),
        anEmptyLine,
        aSimpleCommitLog(),
        anEmptyLine,
        aMergeCommitLog(),
        anEmptyLine,
        aSimpleCommitLog(),
        anEmptyLine,
        aMergeCommitLog(),
        anEmptyLine,
        aMergeCommitLog(),
        anEmptyLine,
        anEmptyLine
      )
        .through(CommitLogParser[IO].parse)
        .compile
        .toList

    assertIO(
      obtained = res.map(_.size),
      returns  = 6
    )
  }

  def aSimpleCommitLog(
                        id: CommitId         = aCommitId,
                        author: CommitAuthorId = aCommitAuthor,
                        date: CommitDate     = aCommitDate,
                        msg: CommitMessage   = aCommitMessage
  ): String =
    s"""commit ${id.value}
       |Author: ${author.value}
       |Date:   ${date.value}
       |
       |    ${msg.value}
       |
       |""".stripMargin

  def aMergeCommitLog(
                       id: CommitId                = aCommitId,
                       merge: (CommitId, CommitId) = (aCommitId, aCommitId),
                       author: CommitAuthorId        = aCommitAuthor,
                       date: CommitDate            = aCommitDate,
                       msg: CommitMessage          = aCommitMessage
  ): String =
    s"""commit ${id.value}
       |Merge: ${merge._1.value} ${merge._2.value}
       |Author: ${author.value}
       |Date:   ${date.value}
       |
       |    ${msg.value}
       |
       |""".stripMargin
}
