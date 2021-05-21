package sbtschemaupdater

import io.github.davidmweber.FlywayPlugin

import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger

object SchemaUpdater extends AutoPlugin {
  override def requires: Plugins = FlywayPlugin

  object autoImport {
    val schemaUpdate =
      taskKey[Seq[File]]("Generates schemas based on migrated database.")
    val schemaUpdateDbUrl    = settingKey[String]("JDBC url to database.")
    val schemaUpdateDbDriver = settingKey[String]("JDBC driver for database.")
    val schemaUpdateDbProfile =
      settingKey[String]("Slick profile for database.")
    val schemaUpdateOutputPackage =
      settingKey[String]("Package to generate schema to.")
  }

  import autoImport._

  import FlywayPlugin.autoImport.{flywayLocations, flywayUrl}

  override lazy val projectSettings = Seq(
    Compile / schemaUpdate := {
      generateSchema(
        Config(
          (Compile / schemaUpdateDbUrl).value,
          (Compile / schemaUpdateDbDriver).value,
          (Compile / schemaUpdateDbProfile).value,
          (Compile / schemaUpdateOutputPackage).value,
        ),
        (Compile / dependencyClasspath).value,
        (Compile / sourceManaged).value,
        runner.value,
        streams.value.log,
      )
    },
    Test / schemaUpdate := {
      generateSchema(
        Config(
          (Test / schemaUpdateDbUrl).value,
          (Test / schemaUpdateDbDriver).value,
          (Test / schemaUpdateDbProfile).value,
          (Test / schemaUpdateOutputPackage).value,
        ),
        (Test / dependencyClasspath).value,
        (Test / sourceManaged).value,
        runner.value,
        streams.value.log,
      )
    },
    Compile / schemaUpdateDbUrl := (Compile / flywayUrl).value,
    Test / schemaUpdateDbUrl := (Test / flywayUrl).value,
    Test / flywayLocations := flywayLocations.value,
    Compile / schemaUpdateOutputPackage := schemaUpdateOutputPackage.value,
    Test / schemaUpdateOutputPackage := schemaUpdateOutputPackage.value,
  )

  private case class Config(
      url: String,
      driver: String,
      profile: String,
      outputPkg: String)

  private def generateSchema(
      config: Config,
      classpath: Classpath,
      outputDir: File,
      runner: ScalaRun,
      logger: ManagedLogger,
    ): Seq[File] = {
    runner
      .run(
        "slick.codegen.SourceCodeGenerator",
        classpath.files,
        Seq(
          config.profile,
          config.driver,
          config.url,
          outputDir.getPath,
          config.outputPkg,
        ),
        logger,
      )
      .get
    Seq(outputDir / config.outputPkg.replace('.', '/') / "Tables.scala")
  }
}
