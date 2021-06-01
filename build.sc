import mill._, scalalib._, scalafmt._

trait ScalaXModule extends ScalaModule with ScalafmtModule {
  def scalacOptions = super.scalacOptions() ++ Seq(
    "-language:postfixOps",
  )
  trait ScalaTestModule extends Tests with TestModule.ScalaTest {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.9")
  }
}

trait Scala3Module extends ScalaXModule {
  def scalaVersion = "3.0.0"
}

trait Scala2Module extends ScalaXModule {
  def scalaVersion = "2.13.6"
}

trait CustomPath extends ScalaModule {
  def basePath: String
  def millSourcePath = super.millSourcePath / os.up / basePath
}

object domain extends Scala3Module {
  def moduleDeps = Seq(util)
  object test extends ScalaTestModule
  object accTest extends ScalaTestModule with CustomPath {
    def basePath = "acceptance-test"
  }
}

object util extends Scala3Module

import $ivy.`com.lihaoyi::mill-contrib-flyway:$MILL_VERSION`
import contrib.flyway.FlywayModule

object repo extends Scala2Module with CustomPath {
  def basePath   = "repository"
  def moduleDeps = Seq(schemas)
  def ivyDeps = Agg(
    ivy"com.typesafe.slick::slick:3.3.3",
    ivy"org.slf4j:slf4j-nop:2.0.0-alpha1",
    ivy"com.typesafe.slick::slick-hikaricp:3.3.3",
    ivy"com.h2database:h2:1.4.200",
  )
  object test extends ScalaTestModule with FlywayModule {
    def flywayUrl =
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    def flywayDriverDeps = Agg(
      ivy"com.h2database:h2:1.4.200",
    )
    def flywayFileLocations = T.sources { repo.millSourcePath / "migrations" }
    def test(args: String*) = T.command {
      flywayMigrate()()
      super.test(args: _*)()
    }
  }
  object schemas extends Scala2Module with SchemaUpdateModule {
    def schemaUpdateMigrations = T.sources {
      repo.millSourcePath / "migrations"
    }
    def schemaUpdateOutputPackage = "bujo.repository.schema"
  }
}

trait SchemaUpdateModule extends ScalaModule with FlywayModule {
  def schemaUpdateMigrations: define.Sources = T.sources {
    resources().head.path / "db/migrations"
  }
  override def flywayFileLocations         = schemaUpdateMigrations()
  def schemaUpdateOutputPackage: T[String] = "generated.schema"
  def schemaUpdateDbUrl: T[String]         = s"""jdbc:h2:file:${T.dest / "schema.db"}"""
  override def flywayUrl                   = schemaUpdateDbUrl()
  override def flywayDriverDeps = Agg(
    ivy"com.h2database:h2:1.4.200",
  )
  def compileIvyDeps: T[Agg[Dep]] = Agg(
    ivy"com.typesafe.slick::slick-codegen:3.3.3",
    ivy"org.slf4j:slf4j-nop:2.0.0-alpha1",
  ) ++ flywayDriverDeps()
  private def schemaUpdateClasspath = T {
    Lib.resolveDependencies(
      repositoriesTask(),
      Lib.depToDependency(_, scalaVersion()),
      compileIvyDeps(),
    )
  }
  def schemaUpdate = T.persistent {
    val NO_MIGRATION_APPLIED = 0
    if (flywayMigrate()() != NO_MIGRATION_APPLIED) {
      T.log.info("Updating")
      os.proc(
        "java",
        "-cp",
        schemaUpdateClasspath()
          .map(_.path)
          .mkString(System.getProperty("path.separator")),
        "slick.codegen.SourceCodeGenerator",
        "slick.jdbc.H2Profile",
        "org.h2.Driver",
        schemaUpdateDbUrl(),
        T.dest.toString,
        schemaUpdateOutputPackage(),
      ).call()
    }
    PathRef(T.dest)
  }
  def generatedSources = T.sources { schemaUpdate().path }
}
