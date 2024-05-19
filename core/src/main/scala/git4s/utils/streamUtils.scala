package git4s.utils

import fs2.Stream.ToPull
import fs2.{Chunk, Pull}

extension [F[_], O](toPull: ToPull[F, O])
  def unconsUntil(f: O => Boolean): Pull[F, Nothing, (Chunk[O], fs2.Stream[F, O])] = {

    def go(toPull: ToPull[F, O], buffer: Chunk[O]): Pull[F, Nothing, (Chunk[O], fs2.Stream[F, O])] =
      toPull.uncons1.flatMap {
        case Some((el, rms)) if f(el) =>
          go(rms.pull, buffer ++ Chunk.singleton(el))
        case Some((el, rms)) =>
          Pull.pure((buffer, fs2.Stream(el) ++ rms))
        case None =>
          Pull.pure((buffer, fs2.Stream.empty))
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
