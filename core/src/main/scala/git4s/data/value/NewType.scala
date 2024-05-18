package git4s.data.value

trait NewType[V, T](using env1: V =:= T, env2: T =:= V):
  type Boxed   = T
  type Unboxed = V

  final def apply(value: V): T = value

  extension (t: T) def value: V = t
