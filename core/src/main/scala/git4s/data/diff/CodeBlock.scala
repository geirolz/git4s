package git4s.data.diff

import cats.Show
import fs2.Chunk

case class CodeBlock(
  startLine: Int,
  linesCount: Int,
  lines: Chunk[String] = Chunk.empty
):

  def addChangeLine(line: String): CodeBlock =
    addLines(Chunk(line))

  def addLines(lines: Chunk[String]): CodeBlock =
    copy(lines = this.lines ++ lines)

object CodeBlock:

  def withoutInfo(lines: Chunk[String]): CodeBlock =
    CodeBlock(0, 0, lines)

  val empty: CodeBlock = CodeBlock.withoutInfo(Chunk.empty)

  given Show[CodeBlock] = Show.fromToString
