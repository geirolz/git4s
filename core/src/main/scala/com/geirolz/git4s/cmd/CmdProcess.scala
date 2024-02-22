package com.geirolz.git4s.cmd

import fs2.{Pipe, Stream}
import fs2.io.process.Process

trait CmdProcess[F[_]]:
  def isAlive: F[Boolean]
  def exitValue: F[Int]
  def stdin: Pipe[F, Byte, Nothing]
  def stdout: Stream[F, Byte]
  def stderr: Stream[F, Byte]

object CmdProcess:

  def apply[F[_]](
    _isAlive: => F[Boolean],
    _exitValue: => F[Int],
    _stdin: => Pipe[F, Byte, Nothing],
    _stdout: => Stream[F, Byte],
    _stderr: => Stream[F, Byte]
  ): CmdProcess[F] =
    new CmdProcess[F]:
      override def isAlive: F[Boolean]           = _isAlive
      override def exitValue: F[Int]             = _exitValue
      override def stdin: Pipe[F, Byte, Nothing] = _stdin
      override def stdout: Stream[F, Byte]       = _stdout
      override def stderr: Stream[F, Byte]       = _stderr

  def fromProcess[F[_]](p: Process[F]): CmdProcess[F] =
    CmdProcess(
      _isAlive   = p.isAlive,
      _exitValue = p.exitValue,
      _stdin     = p.stdin,
      _stdout    = p.stdout,
      _stderr    = p.stderr
    )
