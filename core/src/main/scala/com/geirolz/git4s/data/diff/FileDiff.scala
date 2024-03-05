package com.geirolz.git4s.data.diff

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.geirolz.git4s.codec.CmdDecoder.Result
import com.geirolz.git4s.codec.DecodingFailure.ParsingFailure
import com.geirolz.git4s.codec.{CmdDecoder, DecodingFailure}
import com.geirolz.git4s.diff.FileDiffParser
import fs2.io.file.Path
import fs2.{Chunk, Pull}

sealed trait FileDiff(
  sourceFile: Option[Path],
  targetFile: Option[Path],
  changes: LazyList[(CodeBlock, CodeBlock)]
):
  def isNewFile: Boolean      = this.isInstanceOf[FileDiff.NewFile]
  def isDeletedFile: Boolean  = this.isInstanceOf[FileDiff.DeletedFile]
  def isModifiedFile: Boolean = this.isInstanceOf[FileDiff.ModifiedFile]
  def isRenamedFile: Boolean  = this.isInstanceOf[FileDiff.RenamedFile]

object FileDiff extends FileDiffInstances:

  case class NewFile(
    newFile: Path,
    content: CodeBlock
  ) extends FileDiff(None, Some(newFile), LazyList((CodeBlock.empty, content)))
  object NewFile:
    def empty(path: Path): NewFile = NewFile(path, CodeBlock.empty)

  case class DeletedFile(
    sourceFile: Path,
    content: CodeBlock
  ) extends FileDiff(Some(sourceFile), None, LazyList((content, CodeBlock.empty)))

  case class ModifiedFile(
    sourceFile: Path,
    newFile: Path,
    changes: LazyList[(CodeBlock, CodeBlock)]
  ) extends FileDiff(Some(sourceFile), Some(newFile), changes):

    def addChanges(changes: (CodeBlock, CodeBlock)): ModifiedFile =
      this.copy(changes = changes #:: this.changes)

  case class RenamedFile(
    sourceFile: Path,
    newFile: Path
  ) extends FileDiff(Some(sourceFile), Some(newFile), LazyList.empty)

sealed trait FileDiffInstances:

  given [F[_]: Async: FileDiffParser]: CmdDecoder[F, FileDiff] =
    CmdDecoder.lines(stream =>
      FileDiffParser[F]
        .parse(stream)
        .attempt
        .map(_.leftMap(e => ParsingFailure(e.getMessage)))
    )
