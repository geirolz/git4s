package com.geirolz.git4s.data.value

opaque type CommitId = String
object CommitId:
  def apply(value: String): CommitId         = value
  extension (id: CommitId) def value: String = id

opaque type CommitDate = String
object CommitDate:
  def apply(value: String): CommitDate         = value
  extension (id: CommitDate) def value: String = id

opaque type CommitMessage = String
object CommitMessage:
  def apply(value: String): CommitMessage         = value
  extension (id: CommitMessage) def value: String = id
