  // errors
  // changes types
  private sealed trait ChangeType(val symbol: String)
  private object ChangeType:
    case object NewLine extends ChangeType("+")
    case object DeletedLine extends ChangeType("-")

        // ------------------ EDITED FILE ------------------
        def goChangeBlock(
          tpe: ChangeType,
          source: fs2.Stream[F, String]
        ): Pull[F, Nothing, (CodeBlock, fs2.Stream[F, String])] =
          source.groupAdjacentBy(_.startsWith(tpe.symbol)).pull.uncons1.flatMap {
            case Some(((_, lines), rms)) =>
              val codeBlock = CodeBlock.withoutInfo(lines.map(_.drop(1)))
              Pull.pure((codeBlock, source.drop(lines.size)))
            case None =>
              // empty file - stream ended
              Pull.pure((CodeBlock.empty, fs2.Stream.empty))
          }

        def goNewOrDeletedFile(tpe: ChangeType)(
          val isNewFile: Boolean = tpe == ChangeType.NewLine
          val f: (Path, CodeBlock) => NewFile | DeletedFile = tpe match
            case ChangeType.NewLine     => NewFile(_, _)
            case ChangeType.DeletedLine => DeletedFile(_, _)
                  val path = if (isNewFile) pathA else pathB
                  goChangeBlock(tpe, rms2).flatMap { case (changes, rms3) =>
                    val fileDiff: NewFile | DeletedFile =
                      f(
                        Path(path),
                        changes.copy(
                          startLine   = sline.toInt,
                          startColumn = scol.toInt,
                          endLine     = eline.toInt,
                          endColumn   = ecol.toInt
                      )
                    Pull.pure((fileDiff, rms3))
              goNewOrDeletedFile(ChangeType.NewLine)(rms).flatMap { case (newFile, rms2) =>
              goNewOrDeletedFile(ChangeType.DeletedLine)(rms)