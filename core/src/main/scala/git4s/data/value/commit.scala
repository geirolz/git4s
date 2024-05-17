package git4s.data.value

opaque type CommitId = String
object CommitId extends NewType[String, CommitId]

opaque type CommitAuthor = String
object CommitAuthor extends NewType[String, CommitAuthor]

opaque type CommitDate = String
object CommitDate extends NewType[String, CommitDate]

opaque type CommitMessage = String
object CommitMessage extends NewType[String, CommitMessage]
