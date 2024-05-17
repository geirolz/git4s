package git4s.log

import cats.effect.kernel.{Async, Concurrent}
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.Path

import java.time.LocalDateTime

trait LogFormatter:

  def format[F[_]: Async](
    compiledCmd: String,
    in: Stream[F, String],
    workingDir: Option[Path],
    exitCode: Int,
    datetime: Option[LocalDateTime] = None,
    verbose: Boolean                = false
  ): F[String]

object LogFormatter:

  given LogFormatter = new LogFormatter:
    override def format[F[_]: Async](
      compiledCmd: String,
      in: Stream[F, String],
      workingDir: Option[Path],
      exitCode: Int,
      datetime: Option[LocalDateTime] = None,
      verbose: Boolean                = false
    ): F[String] =
      val dateInfo       = datetime.fold("")(d => s"${d.toString} - ")
      val workingDirInfo = workingDir.fold("")(p => s" in $p")

      val inputInfoF: F[String] =
        if (!verbose)
          "".pure[F]
        else
          in.compile.string.map(s => s"Input: $s\n")

      inputInfoF.map { inputInfo =>
        s"""|$dateInfo"$compiledCmd" exited with code $exitCode$workingDirInfo
            |$inputInfo""".stripMargin
      }
