package git4s.data.value

opaque type RepositoryRef = String
object RepositoryRef extends NewType[String, RepositoryRef]
