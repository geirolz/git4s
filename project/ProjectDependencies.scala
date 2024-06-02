import sbt.*
import scala.language.postfixOps

object ProjectDependencies {

  // base
  private val catsVersion       = "2.10.0"
  private val catsEffectVersion = "3.5.4"
  private val fs2Version        = "3.10.2"
  // test
  private val munitVersion       = "1.0.0"
  private val munitEffectVersion = "2.0.0"
  private val scalacheck         = "1.18.0"

  lazy val common: Seq[ModuleID] = Seq(
    // base
    "org.typelevel" %% "cats-core"   % catsVersion,
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "co.fs2"        %% "fs2-core"    % fs2Version,
    "co.fs2"        %% "fs2-io"      % fs2Version,

    // test
    "org.scalameta"  %% "munit"             % munitVersion       % Test,
    "org.typelevel"  %% "munit-cats-effect" % munitEffectVersion % Test,
    "org.scalameta"  %% "munit-scalacheck"  % munitVersion       % Test,
    "org.scalacheck" %% "scalacheck"        % scalacheck         % Test
  )

  object Core {
    lazy val dedicated: Seq[ModuleID] = Nil
  }

  object Plugins {
    val compilerPlugins: Seq[ModuleID] = Nil
  }

  object Docs {
    lazy val dedicated: Seq[ModuleID] = Nil
  }
}
