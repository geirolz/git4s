package git4s.data.value

type BranchName = String
object Branch extends NewType[String, BranchName]:
  val main: BranchName = "main"
