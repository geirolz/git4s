package git4s.data

import cats.ApplicativeThrow
import git4s.data.GitProvider.{BitBucket, GitHub, GitLab}

import scala.util.Try

opaque type RepositoryURL = String
object RepositoryURL:

  def fromStringF[F[_]: ApplicativeThrow](value: String): F[RepositoryURL] =
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

  def fromString(value: String): Option[RepositoryURL] =
    value match
      case s"https://${VStr(username)}:${VStr(password)}@${VStr(provider)}/${VStr(org)}/${VStr(repo)}.git" =>
        Some(value)
      case s"https://${VStr(provider)}/${VStr(org)}/${VStr(repo)}.git" =>
        Some(value)
      case s"git@${VStr(provider)}:${VStr(org)}/${VStr(repo)}.git" =>
        Some(value)
      case _ =>
        None

  def unsafeFromString(value: String): RepositoryURL =
    fromStringF[Try](value).get

  extension (url: RepositoryURL) def value: String = url

  lazy val github: BuilderProviderSelected    = builder(GitHub.host)
  lazy val gitlab: BuilderProviderSelected    = builder(GitLab.host)
  lazy val bitbucket: BuilderProviderSelected = builder(BitBucket.host)

  // builder
  def builder(provider: String): BuilderProviderSelected =
    BuilderProviderSelected(provider)

  class BuilderProviderSelected private[RepositoryURL] (provider: String):
    def repository(organization: String, repository: String): RepositoryURLBuilderAllSelected =
      RepositoryURLBuilderAllSelected(provider, s"$organization/$repository")
    def repository(repositoryRef: String): RepositoryURLBuilderAllSelected =
      RepositoryURLBuilderAllSelected(provider, repositoryRef)

  class RepositoryURLBuilderAllSelected private[RepositoryURL] (provider: String, repositoryRef: String) {
    def https: RepositoryURLBuilder =
      RepositoryURLBuilder(s"https://$provider/$repositoryRef.git")

    def httpsAuth(username: String, password: String): RepositoryURLBuilder =
      RepositoryURLBuilder(s"https://$username:$password@$provider/$repositoryRef.git")

    def ssh: RepositoryURLBuilder =
      RepositoryURLBuilder(s"git@$provider:$repositoryRef.git")
  }

  class RepositoryURLBuilder private[RepositoryURL] (url: String) {
    def apply[F[_]: ApplicativeThrow]: F[RepositoryURL] = fromStringF(url)
    def option: Option[RepositoryURL]                   = fromString(url)
    def unsafe: RepositoryURL                           = unsafeFromString(url)
  }
