package git4s.data.value

opaque type CommitId = String
object CommitId extends NewType[String, CommitId]

opaque type CommitAuthorId = String
object CommitAuthorId extends NewType[String, CommitAuthorId]

opaque type CommitAuthorEmail = String
object CommitAuthorEmail extends NewType[String, CommitAuthorEmail]

opaque type CommitDate = String
object CommitDate extends NewType[String, CommitDate]

opaque type CommitMessage = String
object CommitMessage extends NewType[String, CommitMessage]
