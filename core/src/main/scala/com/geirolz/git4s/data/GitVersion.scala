package com.geirolz.git4s.data

import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder
import com.geirolz.git4s.codec.DecodingFailure.GenericDecodingFailure

case class GitVersion(major: Int, minor: Int, patch: Int):
  override def toString: String = s"$major.$minor.$patch"

object GitVersion:

  given CmdDecoder[GitVersion] = CmdDecoder.instance {
    _.trim match
      case s"git version $major.$minor.$patch" =>
        GitVersion(major.toInt, minor.toInt, patch.toInt).asRight
      case result =>
        GenericDecodingFailure(
          s"""Invalid version format.
                |Expected: git version <major>.<minor>.<patch>
                |Got: [$result]""".stripMargin
        ).asLeft

  }
