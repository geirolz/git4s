package git4s.data

import cats.effect.kernel.Async

trait OperativeSystem
object OperativeSystem:
  case object Windows extends OperativeSystem
  case object Linux extends OperativeSystem
  case object MacOS extends OperativeSystem

  def getCurrent[F[_]: Async]: F[OperativeSystem] =
    Async[F].delay {
      System.getProperty("os.name").toLowerCase match
        case x if x.contains("win")                                           => Windows
        case x if x.contains("nix") || x.contains("nux") || x.contains("aix") => Linux
        case x if x.contains("mac")                                           => MacOS
        case _ => throw new RuntimeException("Unsupported operative system")
    }
