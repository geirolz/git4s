package git4s.data.value

type Arg = String
object Arg extends NewType[String, Arg]

opaque type CmdArg = String
object CmdArg extends NewType[String, CmdArg]:
  extension (ctx: StringContext) inline def cmd(v: Any*): CmdArg = CmdArg(ctx.s(v*))
