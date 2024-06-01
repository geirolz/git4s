package git4s.data

trait GitProvider:
  def host: String

object GitProvider:

  case object GitHub extends GitProvider:
    val host = "github.com"

  case object GitLab extends GitProvider:
    val host = "gitlab.com"

  case object BitBucket extends GitProvider:
    val host = "bitbucket.org"
