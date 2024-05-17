package git4s.data.value

type Remote = String
object Remote extends NewType[String, Remote]:
  val origin: Remote = "origin"
