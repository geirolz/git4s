package com.geirolz.git4s.data

import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder
import fs2.io.file.Path

sealed trait GitInitResult
object GitInitResult:

  case class GitInitGenericResult(value: String) extends GitInitResult
  case class InitializedEmptyRepository(path: Path) extends GitInitResult
  case class ReinitializedExistingRepository(path: Path) extends GitInitResult

  given  CmdDecoder[GitInitResult] =
    CmdDecoder.text.map {
      case s"Initialized empty Git repository in $path" =>
        InitializedEmptyRepository(Path(path))
      case s"Reinitialized existing Git repository in $path" =>
        ReinitializedExistingRepository(Path(path))
      case value =>
        GitInitGenericResult(value)
    }
