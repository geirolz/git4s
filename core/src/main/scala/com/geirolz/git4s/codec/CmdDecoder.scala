package com.geirolz.git4s.codec

import cats.effect.kernel.Async
import com.geirolz.git4s.codec.CmdDecoder.instance
import fs2.{Pipe, Stream}

trait CmdDecoder[F[_], T]:
  def decode: Pipe[F, String, CmdDecoder.Result[T]]
  def map[U](f: T => U): CmdDecoder[F, U]
  def emap[U](f: T => CmdDecoder.Result[U]): CmdDecoder[F, U]

object CmdDecoder extends ProcessDecoderInstances:

  type Result[T] = Either[DecodingFailure, T]

  inline def apply[F[_], T](using d: CmdDecoder[F, T]): CmdDecoder[F, T] = d

  inline def success[F[_]: Async, T](t: T): CmdDecoder[F, T] =
    const(Right(t))

  inline def failed[F[_]: Async, T](e: DecodingFailure): CmdDecoder[F, T] =
    const(Left(e))

  inline def const[F[_]: Async, T](result: CmdDecoder.Result[T]): CmdDecoder[F, T] =
    instance[F, T](_.drain.merge(Stream.emit(result).covary[F]))

  def lines[F[_]: Async, T](pipe: Pipe[F, String, CmdDecoder.Result[T]]): CmdDecoder[F, T] =
    instance(_.through(fs2.text.lines[F]).through(pipe))

  def instance[F[_]: Async, T](pipe: Pipe[F, String, CmdDecoder.Result[T]]): CmdDecoder[F, T] =
    new CmdDecoder[F, T]:

      override def decode: Pipe[F, String, CmdDecoder.Result[T]] =
        pipe

      override def map[U](f: T => U): CmdDecoder[F, U] =
        instance(s => decode(s).map(_.map(f)))

      override def emap[U](f: T => Result[U]): CmdDecoder[F, U] =
        instance(s => decode(s).map(_.flatMap(f)))

transparent sealed trait ProcessDecoderInstances:

  given unit[F[_]: Async]: CmdDecoder[F, Unit] =
    CmdDecoder.success(())

  given text[F[_]: Async]: CmdDecoder[F, String] =
    instance(_.reduceSemigroup[String].map(Right(_)))
