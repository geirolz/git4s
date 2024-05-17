package git4s.data

import cats.effect.kernel.Async
import cats.syntax.all.*
import git4s.codec.DecodingFailure.GenericDecodingFailure
import git4s.codec.CmdDecoder

case class GitVersion(major: Int, minor: Int, patch: Int):
  override def toString: String = s"$major.$minor.$patch"

object GitVersion:

  given [F[_]: Async]: CmdDecoder[F, GitVersion] =
    CmdDecoder.text[F].emap {
      case s"git version $major.$minor.$patch" =>
        Right(GitVersion(major.toInt, minor.toInt, patch.toInt))
      case result =>
        Left(
          GenericDecodingFailure(
            s"""Invalid version format.
                |Expected: git version <major>.<minor>.<patch>
                |Got: [$result]""".stripMargin
          )
        )
    }
