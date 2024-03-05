package com.geirolz.git4s.data.diff

import cats.Show
import fs2.Chunk

case class CodeBlock(
  startLine: Int,
  startColumn: Int,
  endLine: Int,
  endColumn: Int,
  lines: Chunk[String] = Chunk.empty
):

  def addChangeLine(line: String): CodeBlock =
    addLines(Chunk(line))

  def addLines(lines: Chunk[String]): CodeBlock =
    copy(lines = this.lines ++ lines)

  override def toString: String =
    s"""|-$startColumn:$startLine +$endColumn:$endLine,
       |${lines.toList.mkString("\n")}""".stripMargin

object CodeBlock:
  
  def withoutInfo(lines: Chunk[String]): CodeBlock = 
    CodeBlock(0, 0, 0, 0, lines)
    
  val empty: CodeBlock = CodeBlock(0, 0, 0, 0, Chunk.empty)

  given Show[CodeBlock] = Show.fromToString
