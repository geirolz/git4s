package com.geirolz.git4s.utils

import cats.effect.kernel.Async
import com.geirolz.git4s.codec.{CmdDecoder, DecodingFailure}
import com.geirolz.git4s.data.value.{Author, CommitId}
import fs2.Stream.ToPull
import fs2.{Chunk, Pull}

extension [F[_], O](toPull: ToPull[F, O])
  def unconsUntil(f: O => Boolean): Pull[F, Nothing, Option[(Chunk[O], fs2.Stream[F, O])]] = {

    def go(toPull: ToPull[F, O], buffer: Chunk[O]): Pull[F, Nothing, Option[(Chunk[O], fs2.Stream[F, O])]] =
      toPull.uncons1.flatMap {
        case Some((el, rms)) if f(el) =>
          go(rms.pull, buffer ++ Chunk.singleton(el))
        case Some((el, rms)) =>
          Pull.pure(Some((buffer, rms)))
        case None =>
          Pull.pure(None)
      }

    go(toPull, Chunk.empty)
  }

  def unconsUnless(f: O => Boolean): Pull[F, Nothing, Option[(Chunk[O], fs2.Stream[F, O])]] = {

    def go(toPull: ToPull[F, O], buffer: Chunk[O]): Pull[F, Nothing, Option[(Chunk[O], fs2.Stream[F, O])]] =
      toPull.uncons1.flatMap {
        case Some((el, rms)) if f(el) =>
          Pull.pure(Some((buffer, fs2.Stream(el) ++ rms)))
        case Some((el, rms)) =>
          go(rms.pull, buffer ++ Chunk.singleton(el))
        case None =>
          Pull.pure(Some((buffer, fs2.Stream.empty)))
      }

    go(toPull, Chunk.empty)
  }
