package git4s.log

opaque type LogFilter = Int => Boolean
object LogFilter:
  def apply(f: Int => Boolean): LogFilter             = f
  extension (f: LogFilter) def apply(i: Int): Boolean = f(i)

  // instances
  val onlyFailures: LogFilter = _ != 0
  val all: LogFilter          = _ => true
