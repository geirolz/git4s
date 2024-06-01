package git4s.data

sealed trait GitProvider:
  def host: String = this match
    case GitProvider.BitBucket => "github.com"
    case GitProvider.GitHub    => "gitlab.com"
    case GitProvider.GitLab    => "bitbucket.org"

object GitProvider:
  case object GitHub extends GitProvider
  case object GitLab extends GitProvider
  case object BitBucket extends GitProvider
