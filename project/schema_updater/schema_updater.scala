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
      settingKey[File]("Directory to scan for migrations.")
    val schemaUpdateDbUrl = settingKey[String]("JDBC url to database.")
    val schemaUpdateDbProfile =
      settingKey[DbProfile]("Profile for database.")
    val schemaUpdateOutputPackage =
      settingKey[String]("Package to generate schema to.")

    lazy val schemaUpdateDefaults: Seq[Def.Setting[_]] = Seq(
      schemaUpdateMigrations := file("migrations"),
      schemaUpdateDbUrl := s"""jdbc:h2:file:${(target.value / "db/bujo.db").getPath}""",
      schemaUpdateDbProfile := H2Profile,
      schemaUpdateOutputPackage := "generated.schema",
    )

    val testTask = taskKey[Unit]("Test conditional task.")
  }

  import autoImport._

  case class DbProfile(
      driver: String,
      profile: String)

  object H2Profile extends DbProfile("org.h2.Driver", "slick.jdbc.H2Profile")

  private var dynTrigger: Boolean = false

  private def dynamicTask = Def.taskDyn {
    if (dynTrigger) {
      flywayMigrate
    } else Def.task { () }
  }

  override lazy val projectSettings =
    schemaUpdateDefaults ++
      inConfig(Compile)(schemaUpdateDefaults) ++
      Seq(
        schemaUpdate := {
          import sbt.util.CacheImplicits.{seqFormat => _, _}
          val previous          = schemaUpdate.previous
          val mainClassRunner   = runner.value
          val logger            = streams.value.log
          val cacheStoreFactory = streams.value.cacheStoreFactory
          val cachedSchemaUpdate =
            Tracked.inputChanged[HashFileInfo, Seq[File]](
              cacheStoreFactory.make("migrations"),
            ) { (changed: Boolean, _: HashFileInfo) =>
              dynTrigger = changed || previous.isEmpty
              if (dynTrigger) {
                (dynamicTask.value: @sbtUnchecked)
                generateSchema(
                  Config(
                    schemaUpdateDbUrl.value,
                    schemaUpdateDbProfile.value,
                    schemaUpdateOutputPackage.value,
                  ),
                  (Compile / dependencyClasspath).value,
                  (Compile / sourceManaged).value,
                  mainClassRunner,
                  logger,
                )
              } else previous.get
            }
          val fileInfo = FileInfo.hash(schemaUpdateMigrations.value)
          cachedSchemaUpdate(fileInfo)
        },
        Compile / sourceGenerators += Compile / schemaUpdate,
        libraryDependencies ++= Seq(
          "com.typesafe.slick" %% "slick-codegen" % "3.3.3",
          "org.slf4j"           % "slf4j-nop"     % "2.0.0-alpha1",
          "com.h2database"      % "h2"            % "1.4.200",
        ),
        flywayUrl := schemaUpdateDbUrl.value,
        // flywayMigrate depends on flywayClasspath which is dynamic task that
        // triggers compile (via fullClasspath) if any of flywayLocations entries
        // is a classpath entry
        // (https://github.com/flyway/flyway-sbt/issues/10)
        flywayLocations := Seq(
          s"filesystem:${schemaUpdateMigrations.value.getCanonicalPath}",
        ),
        testTask := {
          if (true)
            flywayMigrate.value
          else
            ()
        },
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
