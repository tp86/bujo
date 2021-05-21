import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger

object SchemaUpdater_ {
  case class SchemaUpdaterConfig(
      url: String,
      driver: String,
      profile: String,
      outputPkg: String)
  lazy val schemaUpdate_ =
    taskKey[Seq[File]]("Generates schemas based on migrated database.")
  lazy val schemaUpdateConfig_ = settingKey[SchemaUpdaterConfig](
    "Configuration settings for schema code generator.",
  )
  /*private*/
  def generateSchema(
      config: SchemaUpdaterConfig,
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
