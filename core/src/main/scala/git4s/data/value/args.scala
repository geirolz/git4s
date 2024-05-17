package git4s.data.value

import scala.quoted.{Expr, Quotes}

type Arg = String

opaque type CmdArg = String
object CmdArg:
  inline def apply(app: String): CmdArg = app
  extension (cmdArg: CmdArg) {
    def value: String = cmdArg
  }
  extension (ctx: StringContext) {
    inline def cmd(v: Any*): CmdArg = CmdArg(ctx.s(v*))
  }
