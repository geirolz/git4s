package com.geirolz.git4s.cmd

import cats.effect.kernel.Async
import fs2.io.process.Process
import fs2.{Stream, text}

private[git4s] trait CmdProcess[F[_]]:
  def isAlive: F[Boolean]
  def exitValue: F[Int]
  def stdout: Stream[F, String]
  def stderr: Stream[F, String]

private[git4s] object CmdProcess:
  def fromProcess[F[_]: Async](process: Process[F], in: Stream[F, String]): CmdProcess[F] = new CmdProcess[F]:
    private val inUtf8: Stream[F, Nothing] = in.through(text.utf8.encode).through(process.stdin)
    def isAlive: F[Boolean]                = process.isAlive
    def exitValue: F[Int]                  = process.exitValue
    def stdout: Stream[F, String]          = process.stdout.through(text.utf8.decode).concurrently(inUtf8)
    def stderr: Stream[F, String]          = process.stderr.through(text.utf8.decode)
