package git4s.data.request

type GitConfigKey = String
object GitConfigKey:
  val userName: GitConfigKey  = "user.name"
  val userEmail: GitConfigKey = "user.email"
