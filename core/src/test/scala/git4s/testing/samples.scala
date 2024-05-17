package git4s.testing

import git4s.data.value.{CommitAuthor, CommitDate, CommitId, CommitMessage}

import java.util.UUID
import scala.util.Random

def aDummyLine: String =
  "DUMMY LINE TO IGNORE"

def anEmptyLine: String = " "

def aCommitId: CommitId =
  CommitId(UUID.randomUUID().toString)

def aCommitAuthor: CommitAuthor =
  val id: Int = Random.nextInt()
  CommitAuthor(s"author#$id <author.$id@git.com>")

def aCommitDate: CommitDate =
  CommitDate("Wed Feb 14 15:35:01 2024 +0100")

def aCommitMessage: CommitMessage =
  CommitMessage(s"""
       |Commit Message L1 - #${Random.nextInt()}
       |
       |Commit Message L2 - #${Random.nextInt()}
       |
       |Commit Message L3 - #${Random.nextInt()}
       |""".stripMargin)
