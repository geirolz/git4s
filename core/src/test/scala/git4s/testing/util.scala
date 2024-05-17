package git4s.testing

import cats.effect.IO

def lines(values: String*): fs2.Stream[IO, String] =
  fs2.Stream.emits(values.flatMap(_.split("\n").toSeq))
