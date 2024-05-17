// TODO WIP
private[git4s] trait FileDiffParser[F[_]]:
// TODO WIP
private[git4s] object FileDiffParser:
    case object NewLines extends ChangeType("+")
    case object DeletedLines extends ChangeType("-")
  given [F[_]](using F: Async[F]): FileDiffParser[F] with
    def parse: Pipe[F, String, FileDiff] = (s: fs2.Stream[F, String]) =>

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

      // ------------------ NEW OR DELETED FILE ------------------
      def goNewOrDeletedFile(tpe: ChangeType)(
        stream: fs2.Stream[F, String]
      ): Pull[F, Nothing, (NewFile | DeletedFile, fs2.Stream[F, String])] =

        val isNewFile: Boolean = tpe == ChangeType.NewLines
        val f: (Path, CodeBlock) => NewFile | DeletedFile = tpe match
          case ChangeType.NewLines     => NewFile(_, _)
          case ChangeType.DeletedLines => DeletedFile(_, _)

        stream.pull.unconsN(3).flatMap {
          case Some(chunk, rms2: fs2.Stream[F, String]) =>
            chunk.toList match {
              case s"$idA $_/$pathA" :: s"$idB $_/$pathB" :: s"@@ -$scol,$sline +$ecol,$eline @@" :: Nil =>
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
                }
              case x =>
                Pull.raiseError(MalformedDiff(x.mkString("\n")))
            }
          case _ =>
            Pull.raiseError(PrematureEOS)
        }

      // ------------------ MOVED OR RENAMED FILE ------------------
      def goMovedOrRenamedFile(
        from: String,
        stream: fs2.Stream[F, String]
      ): Pull[F, Nothing, (RenamedFile, fs2.Stream[F, String])] =
        stream.pull.uncons1.flatMap {
          case Some((s"rename to $to", rms2)) =>
            Pull.pure((RenamedFile(Path(from), Path(to)), rms2))
          case Some((x, _)) =>
            Pull.raiseError(MalformedDiff(x))
          case None =>
            Pull.raiseError(PrematureEOS)
        }

      def go(s: fs2.Stream[F, String]): Pull[F, FileDiff, Unit] =
        s.pull.uncons1.flatMap {
          case Some(s"new file mode $_", rms: fs2.Stream[F, String]) =>
            goNewOrDeletedFile(ChangeType.NewLines)(rms.drop(1))
              .flatMap { case (newFile, rms2) => Pull.output1(newFile) >> go(rms2) }

          case Some(s"deleted file mode $_", rms: fs2.Stream[F, String]) =>
            goNewOrDeletedFile(ChangeType.DeletedLines)(rms.drop(1))
              .flatMap { case (newFile, rms2) => Pull.output1(newFile) >> go(rms2) }

          case Some(s"rename from $from", rms: fs2.Stream[F, String]) =>
            goMovedOrRenamedFile(from, rms)
              .flatMap { case (renamedFile, rms2) => Pull.output1(renamedFile) >> go(rms2) }

//          case Some(s"index $_", rms: fs2.Stream[F, String]) =>
//            goNewOrDeletedFile(ChangeType.DeletedLine)(rms)
//              .flatMap { case (newFile, rms2) => Pull.output1(newFile) >> go(rms2) }

          // skip line
          case Some(_, rms) =>
            go(rms)
          case _ =>
            // no more elements
            Pull.done
        }

      go(s).stream