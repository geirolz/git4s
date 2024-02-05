package com.geirolz.git4s.cmd

import fs2.io.file.Path

sealed trait WorkingCtx(private[git4s] val directory: Option[Path])
private[git4s] object WorkingCtx:
  def apply(optDir: Option[Path]): WorkingCtx    = new WorkingCtx(optDir) {}
  def current(using ctx: WorkingCtx): WorkingCtx = ctx
  object global:
    given WorkingCtx = new WorkingCtx(None) {}

private[git4s] def currentWorkingDir(using ctx: WorkingCtx): Option[Path] = ctx.directory
