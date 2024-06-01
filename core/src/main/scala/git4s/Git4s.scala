package git4s

import cats.effect.{Async, IO}
import cats.syntax.all.*
import fs2.io.file.Path
import git4s.cmd.{CmdRunner, GitCmd, WorkingCtx}
import git4s.data.GitVersion
import git4s.data.RepositoryURL
import git4s.logging.CmdLogger
import git4s.module.Git4sConfig

/** Git4s is a pure functional wrapper around the git command line.
  *
  * It provides a set of methods to interact with a git.
  *
  * You can create a Git4s instance using the `apply` method.
  *
  * Logging is done using the `CmdLogger` type class implicitly passed to each method. You can provide your own
  * implementation of `CmdLogger` to log the command output as you like, or use the default one provided by the library.
  * By default, the library uses the `Noop` logger which doesn't log anything since usually these logs are useful just
  * for debugging purpose.
  *
  * Example:
  * {{{
  *  import cats.effect.IO
  *  import git4s.Git4s
  *  import git4s.data.GitVersion
  *  import git4s.log.*
  *
  *  given logger: CmdLogger[IO] = CmdLogger.console[IO](LogFilter.all)
  *  val result: IO[GitVersion] = Git4s[IO].version
  * }}}
  *
  * @tparam F
  *   the effect type
  */
sealed trait Git4s[F[_]] extends GitInstaller[F], Git4sRepository[F]:

  // directory
  /** Set the working directory for the Git4s instance. */
  inline def withWorkingDirectory(dir: String): Git4s[F] = withWorkingDirectory(Path(dir))

  /** Set the working directory for the Git4s instance. */
  def withWorkingDirectory(dir: Path): Git4s[F]

  /** Instruct Git4s to use the current working directory. */
  def withCurrentWorkingDirectory: Git4s[F]

  /** Get the current working directory if set.
    *
    * `None` means current director.
    */
  def workingDirectory: Option[Path]

  // basic commands
  /** Get the version of the installed git
    *
    * [[https://git-scm.com/docs/git-version]]
    */
  def help(using CmdLogger[F]): F[String]

  /** Clone a repository into a new directory.
    *
    * This method is not under [[Git4sRepository]] because it's not related to a specific repository.
    *
    * [[https://git-scm.com/docs/git-clone]]
    */
  def clone(repository: RepositoryURL, directory: Path)(using CmdLogger[F]): F[Unit]

  /** Same as `clone(RepositoryURL, Path)` but with a string URL */
  def clone(repository: String, directory: Path)(using CmdLogger[F], DummyImplicit): F[Unit]

  /** Get a type to access Git local config */
  def localConfig: Git4sConfig[F]

  /** Get a type to access Git global config */
  def globalConfig: Git4sConfig[F]

object Git4s:

  inline def apply[F[_]: Git4s]: Git4s[F] = summon[Git4s[F]]
  given Git4s[IO]                         = Git4s.default[IO]

  def default[F[_]: Async: CmdRunner]: Git4s[F] =
    Git4s.of[F](
      workingDir = None,
      installer  = GitInstaller.default[F]
    )

  def of[F[_]: Async: CmdRunner](
    workingDir: Option[Path] = None,
    installer: GitInstaller[F]
  ): Git4s[F] =
    new Git4s[F]:

      private given WorkingCtx                    = WorkingCtx(workingDir)
      private val repository: Git4sRepository[F]  = Git4sRepository[F]
      override val workingDirectory: Option[Path] = WorkingCtx.current.directory

      export installer.*
      export repository.*

      override def withWorkingDirectory(dir: Path): Git4s[F] =
        Git4s.of(workingDir = Some(dir), installer)

      override def withCurrentWorkingDirectory: Git4s[F] =
        Git4s.of(workingDir = None, installer)

      override def help(using CmdLogger[F]): F[String] =
        GitCmd.help.runGetLast.map(_.value)

      override def clone(repositoryURL: RepositoryURL, destination: Path)(using CmdLogger[F]): F[Unit] =
        GitCmd.clone(repositoryURL, destination).runGetLast.void

      override def clone(repositoryURL: String, destination: Path)(using CmdLogger[F], DummyImplicit): F[Unit] =
        RepositoryURL.fromStringF[F](repositoryURL).flatMap(clone(_, destination))

      override def localConfig: Git4sConfig[F] =
        Git4sConfig.local[F]

      override def globalConfig: Git4sConfig[F] =
        Git4sConfig.global[F]
