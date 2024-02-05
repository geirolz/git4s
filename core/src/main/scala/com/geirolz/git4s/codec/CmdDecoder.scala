package com.geirolz.git4s.codec

import cats.ApplicativeError
import com.geirolz.git4s.codec.CmdDecoder.instance

trait CmdDecoder[T]:
  def decode(p: String): CmdDecoder.Result[T]
  def map[U](f: T => U): CmdDecoder[U]
  def flatMap[U](f: T => CmdDecoder[U]): CmdDecoder[U]
  def handleErrorWith[U >: T](f: DecodingFailure => CmdDecoder[U]): CmdDecoder[U]

object CmdDecoder extends ProcessDecoderInstances:

  type Result[T] = Either[DecodingFailure, T]

  inline def apply[T](using d: CmdDecoder[T]): CmdDecoder[T] = d

  inline def success[T](t: T): CmdDecoder[T] =
    instance[T](_ => Right(t))

  inline def failed[T](e: DecodingFailure): CmdDecoder[T] =
    instance(_ => Left(e))

  def instance[T](f: String => CmdDecoder.Result[T]): CmdDecoder[T] =
    new CmdDecoder[T]:
      override def decode(s: String): CmdDecoder.Result[T] =
        f(s)

      override def map[U](f: T => U): CmdDecoder[U] =
        instance(input => decode(input).map(f))

      override def flatMap[U](f: T => CmdDecoder[U]): CmdDecoder[U] =
        instance(input =>
          decode(input) match
            case Right(value) => f(value).decode(input)
            case Left(e)      => Left(e)
        )

      override def handleErrorWith[U >: T](f: DecodingFailure => CmdDecoder[U]): CmdDecoder[U] =
        instance(input =>
          decode(input) match
            case Left(e) => f(e).decode(input)
            case r       => r
        )

transparent sealed trait ProcessDecoderInstances:

  given unit: CmdDecoder[Unit] =
    CmdDecoder.success(())

  given text: CmdDecoder[String] =
    instance(s => Right(s.trim))

  given [F[_]]: ApplicativeError[CmdDecoder, DecodingFailure] =
    new ApplicativeError[CmdDecoder, DecodingFailure]:

      override def raiseError[A](e: DecodingFailure): CmdDecoder[A] =
        CmdDecoder.failed(e)

      override def handleErrorWith[A](fa: CmdDecoder[A])(f: DecodingFailure => CmdDecoder[A]): CmdDecoder[A] =
        fa.handleErrorWith(f)

      override def pure[A](x: A): CmdDecoder[A] =
        CmdDecoder.success(x)

      override def ap[A, B](ff: CmdDecoder[A => B])(fa: CmdDecoder[A]): CmdDecoder[B] =
        ff.flatMap(f => fa.map(a => f(a)))
