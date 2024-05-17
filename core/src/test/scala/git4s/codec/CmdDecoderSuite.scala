package git4s.codec

import cats.effect.IO
import fs2.Stream
import git4s.codec.CmdDecoder

class CmdDecoderSuite extends munit.CatsEffectSuite {

  test("CmdDecoder.text") {
    for {
      counter <- IO.ref(0)
      stream = Stream
        .emits(
          Seq(
            "Foo",
            "Bar",
            "Baz"
          )
        )
        .covary[IO]
      result <- stream
        .evalTap(_ => counter.update(_ + 1))
        .through(CmdDecoder.text[IO].decode)
        .compile
        .toList
      _ <- assertIO(
        obtained = IO.pure(result),
        returns  = List(Right("FooBarBaz"))
      )
      _ <- assertIO(
        obtained = counter.get,
        returns  = 3
      )
    } yield ()
  }

  test("CmdDecoder.unit") {

    for {

      counter <- IO.ref(0)
      stream = Stream
        .emits(
          Seq(
            "Foo",
            "Bar",
            "Baz"
          )
        )
        .covary[IO]
      result <- stream
        .evalTap(_ => counter.update(_ + 1))
        .through(CmdDecoder.unit[IO].decode)
        .compile
        .toList
      _ <- assertIO(
        obtained = IO.pure(result),
        returns  = List(Right(()))
      )
      _ <- assertIO(
        obtained = counter.get,
        returns  = 3
      )
    } yield ()
  }

  test("CmdDecoder.map") {

    for {

      counter <- IO.ref(0)
      stream = Stream
        .emits(
          Seq(
            "Foo",
            "Bar",
            "Baz"
          )
        )
        .covary[IO]
      result <- stream
        .evalTap(_ => counter.update(_ + 1))
        .through(CmdDecoder.unit[IO].map(_ => "TEST").decode)
        .compile
        .toList
      _ <- assertIO(
        obtained = IO.pure(result),
        returns  = List(Right("TEST"))
      )
      _ <- assertIO(
        obtained = counter.get,
        returns  = 3
      )
    } yield ()
  }

  test("CmdDecoder.emap") {

    for {

      counter <- IO.ref(0)
      stream = Stream
        .emits(
          Seq(
            "Foo",
            "Bar",
            "Baz"
          )
        )
        .covary[IO]
      result <- stream
        .evalTap(_ => counter.update(_ + 1))
        .through(CmdDecoder.unit[IO].emap(_ => Right("TEST")).decode)
        .compile
        .toList
      _ <- assertIO(
        obtained = IO.pure(result),
        returns  = List(Right("TEST"))
      )
      _ <- assertIO(
        obtained = counter.get,
        returns  = 3
      )
    } yield ()
  }
}
