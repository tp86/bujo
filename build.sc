import mill._, scalalib._, scalafmt._

trait AllScalaModule extends ScalaModule with ScalafmtModule

trait Scala3Module extends AllScalaModule {
  def scalaVersion = "3.0.0"
  trait ScalaTestModule extends Tests with TestModule.ScalaTest {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.2.9")
  }
}

trait CustomPath extends ScalaModule {
  def basePath: String
  def millSourcePath = super.millSourcePath / os.up / basePath
}

object domain extends Scala3Module {
  def moduleDeps   = Seq(util)
  object test extends ScalaTestModule
  object accTest extends ScalaTestModule with CustomPath {
    def basePath = "acceptance-test"
  }
}

object util extends Scala3Module

object repo extends Scala3Module with CustomPath {
  def basePath = "repository"
  def moduleDeps = Seq(schemas)
  object schemas extends AllScalaModule with SchemaUpdateModule {
    def scalaVersion = "2.13.6"
    def schemaUpdateMigrations = T.sources { repo.millSourcePath / "migrations" }
    def schemaUpdateOutputPackage = "bujo.repository.schema"
  }
}

import $ivy.`com.lihaoyi::mill-contrib-flyway:$MILL_VERSION`
import contrib.flyway.FlywayModule
trait SchemaUpdateModule extends ScalaModule with FlywayModule {
  def schemaUpdateMigrations: define.Sources = T.sources { resources().head.path / "db/migrations" }
  override def flywayFileLocations = schemaUpdateMigrations()
  def schemaUpdateOutputPackage: T[String] = "generated.schema"
  def schemaUpdateDbUrl: T[String] = s"""jdbc:h2:file:${T.dest / "schema.db"}"""
  override def flywayUrl = schemaUpdateDbUrl()
  override def flywayDriverDeps = Agg(
    ivy"com.h2database:h2:1.4.200"
  )
  def runIvyDeps = Agg(
    ivy"com.typesafe.slick::slick-codegen:3.3.3",
    ivy"org.slf4j:slf4j-nop:2.0.0-alpha1",
  ) ++ flywayDriverDeps()
  def schemaUpdate = T {
    val NO_MIGRATION_APPLIED = 0
    if (flywayMigrate()() != NO_MIGRATION_APPLIED) {
      T.log.info("Updating")
      runMain(
        "slick.codegen.SourceCodeGenerator",
        "slick.jdbc.H2Profile",
        "org.h2.Driver",
        schemaUpdateDbUrl(),
        T.dest.toString,
        schemaUpdateOutputPackage(),
      )
    }
    PathRef(T.dest)
  }
  def generatedSources = T.sources { schemaUpdate().path }
}
