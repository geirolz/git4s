package com.geirolz.git4s.data.value


opaque type CommitId = String
object CommitId:
  def apply(value: String): CommitId         = value
  extension (id: CommitId) def value: String = id
