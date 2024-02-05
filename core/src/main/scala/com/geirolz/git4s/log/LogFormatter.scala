package com.geirolz.git4s.log

import com.geirolz.git4s.cmd.Cmd
import fs2.io.file.Path

import java.time.LocalDateTime

trait LogFormatter:

  // TODO do not use Cmd here, create CmdInfo or something like that
  def format[F[_], E, T](
    cmd: Cmd[F, E, T],
    workingDir: Option[Path],
    exitCode: Int,
    result: String,
    datetime: Option[LocalDateTime] = None
  ): String

object LogFormatter:

  given LogFormatter = new LogFormatter:
    override def format[F[_], E, T](
      cmd: Cmd[F, E, T],
      workingDir: Option[Path],
      exitCode: Int,
      result: String,
      datetime: Option[LocalDateTime] = None
    ): String =
      val dateInfo       = datetime.fold("")(d => s"${d.toString} - ")
      val cmdStr         = cmd.toString
      val workingDirInfo = workingDir.fold("")(p => s" in $p")
      s"""|$dateInfo"$cmdStr" exited with code $exitCode$workingDirInfo
          |$result
          |""".stripMargin
