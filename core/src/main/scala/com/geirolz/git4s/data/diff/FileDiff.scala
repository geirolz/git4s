import com.geirolz.git4s.codec.DecodingFailure.ParsingFailure
import com.geirolz.git4s.diff.FileDiffParser
  given [F[_]: Async: FileDiffParser]: CmdDecoder[F, FileDiff] =
    CmdDecoder.lines(stream =>
      FileDiffParser[F]
        .parse(stream)
        .attempt
        .map(_.leftMap(e => ParsingFailure(e.getMessage)))
    )