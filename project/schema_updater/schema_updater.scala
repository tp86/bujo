package sbtschemaupdater

import io.github.davidmweber.FlywayPlugin

import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger

object SchemaUpdater extends AutoPlugin {
  override def requires = FlywayPlugin
  override def trigger  = noTrigger

  import FlywayPlugin.autoImport._

  object autoImport {
    val schemaUpdate =
      taskKey[Seq[File]]("Generates schemas based on migrated database.")
    val schemaUpdateMigrations =
      settingKey[Seq[String]](
        "Locations (paths as strings) to scan recursively for migrations.",
      )
    val schemaUpdateDbUrl = settingKey[String]("JDBC url to database.")
    val schemaUpdateDbProfile =
      settingKey[DbProfile]("Profile for database.")
    val schemaUpdateOutputPackage =
      settingKey[String]("Package to generate schema to.")

    lazy val schemaUpdateDefaults: Seq[Def.Setting[_]] = Seq(
      schemaUpdateMigrations := Seq("migrations"),
      schemaUpdateDbUrl := "",
      schemaUpdateDbProfile := EmptyProfile,
      schemaUpdateOutputPackage := "generated.schema",
    )
  }

  import autoImport._

  case class DbProfile(
      driver: String,
      profile: String)

  private object EmptyProfile extends DbProfile("", "")
  object H2Profile            extends DbProfile("org.h2.Driver", "slick.jdbc.H2Profile")
  object SqliteProfile
      extends DbProfile("org.sqlite.JDBC", "slick.jdbc.SQLiteProfile")

  override lazy val projectSettings =
    schemaUpdateDefaults ++
      inConfig(Compile)(schemaUpdateDefaults) ++
      Seq(
        schemaUpdate := {
          val _ = flywayMigrate.value
          generateSchema(
            Config(
              schemaUpdateDbUrl.value,
              schemaUpdateDbProfile.value,
              schemaUpdateOutputPackage.value,
            ),
            (Compile / dependencyClasspath).value,
            (Compile / sourceManaged).value,
            runner.value,
            streams.value.log,
          )
        },
        Compile / sourceGenerators += Compile / schemaUpdate,
        schemaUpdateDbUrl := flywayUrl.value,
        libraryDependencies ++= Seq(
          "com.typesafe.slick" %% "slick-codegen" % "3.3.3",
          "org.slf4j"           % "slf4j-nop"     % "2.0.0-alpha1",
        ),
        flywayUrl := schemaUpdateDbUrl.value,
        // flywayMigrate depends on flywayClasspath which is dynamic task that
        // triggers compile (via fullClasspath) if any of flywayLocations entries
        // is a classpath entry
        // (https://github.com/flyway/flyway-sbt/issues/10)
        flywayLocations := schemaUpdateMigrations.value.map(loc =>
          s"filesystem:${loc}",
        ),
      )

  private case class Config(
      url: String,
      profile: DbProfile,
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
          config.profile.profile,
          config.profile.driver,
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
