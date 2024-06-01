package git4s.data

import cats.ApplicativeThrow
import git4s.data.GitProvider.{BitBucket, GitHub, GitLab}

import scala.util.Try

opaque type GitRepositoryURL = String
object GitRepositoryURL:

  def fromStringF[F[_]: ApplicativeThrow](value: String): F[GitRepositoryURL] =
    ApplicativeThrow[F].fromOption(
      oa      = fromString(value),
      ifEmpty = new IllegalArgumentException(s"Invalid RepositoryURL: $value")
    )

  case class VStr(value: String)
  object VStr:
    def unapply(value: String): Option[String] =
      if value.isBlank then None
      else if value.matches("^[a-zA-Z0-9._-]*$") then Some(value)
      else None

  def fromString(value: String): Option[GitRepositoryURL] =
    value match
      case s"https://${VStr(username)}:${VStr(password)}@${VStr(provider)}/${VStr(org)}/${VStr(repo)}.git" =>
        Some(value)
      case s"https://${VStr(provider)}/${VStr(org)}/${VStr(repo)}.git" =>
        Some(value)
      case s"git@${VStr(provider)}:${VStr(org)}/${VStr(repo)}.git" =>
        Some(value)
      case _ =>
        None

  def unsafeFromString(value: String): GitRepositoryURL =
    fromStringF[Try](value).get

  extension (url: GitRepositoryURL) def value: String = url

  lazy val github: BuilderProviderSelected    = builder(GitHub.host)
  lazy val gitlab: BuilderProviderSelected    = builder(GitLab.host)
  lazy val bitbucket: BuilderProviderSelected = builder(BitBucket.host)

  // builder
  def builder(provider: String): BuilderProviderSelected =
    BuilderProviderSelected(provider)

  class BuilderProviderSelected private[GitRepositoryURL] (provider: String):
    def repository(organization: String, repository: String): RepositoryURLBuilderAllSelected =
      RepositoryURLBuilderAllSelected(provider, s"$organization/$repository")
    def repository(repositoryRef: String): RepositoryURLBuilderAllSelected =
      RepositoryURLBuilderAllSelected(provider, repositoryRef)

  class RepositoryURLBuilderAllSelected private[GitRepositoryURL] (provider: String, repositoryRef: String) {
    def https: RepositoryURLBuilder =
      RepositoryURLBuilder(s"https://$provider/$repositoryRef.git")

    def httpsAuth(username: String, password: String): RepositoryURLBuilder =
      RepositoryURLBuilder(s"https://$username:$password@$provider/$repositoryRef.git")

    def ssh: RepositoryURLBuilder =
      RepositoryURLBuilder(s"git@$provider:$repositoryRef.git")
  }

  class RepositoryURLBuilder private[GitRepositoryURL] (url: String) {
    def apply[F[_]: ApplicativeThrow]: F[GitRepositoryURL] = fromStringF(url)
    def option: Option[GitRepositoryURL]                   = fromString(url)
    def unsafe: GitRepositoryURL                           = unsafeFromString(url)
  }
