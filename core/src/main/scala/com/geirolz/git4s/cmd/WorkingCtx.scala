package com.geirolz.git4s.cmd

import fs2.io.file.Path

private[git4s] sealed class WorkingCtx(
  val directory: Option[Path] = None,
  val verboseLog: Boolean     = false
)
private[git4s] object WorkingCtx:
  def apply(optDir: Option[Path]): WorkingCtx    = new WorkingCtx(optDir)
  def current(using ctx: WorkingCtx): WorkingCtx = ctx
  object global:
    given WorkingCtx = new WorkingCtx()

private[git4s] def currentWorkingDir(using ctx: WorkingCtx): Option[Path] = ctx.directory
