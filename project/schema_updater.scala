import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger

object SchemaUpdater {
  case class Config(
      url: String,
      driver: String,
      profile: String,
      outputPkg: String)
  lazy val schemaUpdate =
    taskKey[Seq[File]]("Generates schemas based on migrated database.")
  lazy val schemaUpdateConfig = settingKey[Config](
    "Configuration settings for schema code generator.",
  )
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
          config.url,
          config.driver,
          config.profile,
          outputDir.getPath,
          config.outputPkg,
        ),
        logger,
      )
      .get
    Seq(outputDir / config.outputPkg.replace('.', '/') / "Tables.scala")
  }
}
