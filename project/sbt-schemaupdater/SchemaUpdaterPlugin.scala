package sbtschemaupdater

import sbt._
import sbt.Keys._

object SchemaUpdaterPlugin extends AutoPlugin {
  override def requires = empty
  override def trigger = allRequirements
  
  object autoImport {
    val schemaUpdate = taskKey[Seq[File]]("Updates database and schema objects.")
    val schemaUpdateSlickProfile = settingKey[String]("Slick profile class for database.")
    val schemaUpdateDriver = settingKey[String]("JDBC driver class for database.")
    val schemaUpdateDbUrl = settingKey[String]("JDBC url pointing to database.")
    val schemaUpdatePkg = settingKey[String]("Package where schema objects will be generated.")
  }

  import autoImport._

  override val projectSettings =
    inConfig(Compile)(Seq(
      schemaUpdate := {
        SchemaUpdater((Compile / dependencyClasspath).value, (Compile / sourceManaged).value, runner.value, streams.value.log)
      },
      )) ++
    inConfig(Test)(Seq(
      schemaUpdate := {
        SchemaUpdater((Test / dependencyClasspath).value, (Test / sourceManaged).value, runner.value, streams.value.log)
      },
      ))
}

object SchemaUpdater {
  def apply(cp: Classpath, outputDir: File, runner: ScalaRun, logger: internal.util.ManagedLogger): Seq[File] = {
    runner.run(
      "slick.codegen.SourceCodeGenerator",
      cp.files,
      Seq(
        (schemaUpdate / schemaUpdateSlickProfile).value,
        (schemaUpdate / schemaUpdateDriver).value,
        (schemaUpdate / schemaUpdateDbUrl).value,
        outputDir.getPath,
        (schemaUpdate / schemaUpdatePkg).value,
      ),
      logger,
    )
    .get
    Seq(outputDir / (schemaUpdate / schemaUpdatePkg).value.replace('.', '/') / "Tables.scala")
  }
}
