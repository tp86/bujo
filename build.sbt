import ProjectConfig._
import bloop.integrations.sbt.BloopDefaults
import pureconfig.generic.auto._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.0.0-RC2"

lazy val AccTest = config("acceptance-test") extend (Test)
lazy val accTest = taskKey[Unit]("Executes acceptance tests.")

lazy val scalatest     = "org.scalatest"      %% "scalatest"     % "3.2.7"
lazy val sqliteJdbc    = "org.xerial"          % "sqlite-jdbc"   % "3.34.0"
lazy val slick_codegen = "com.typesafe.slick" %% "slick-codegen" % "3.3.3"

lazy val bujo = (project in file("."))
  .aggregate(domain, repo, util)
  .settings(
    name := "bujo",
  )

lazy val util = project
  .settings(
    name := "bujo-util",
  )

lazy val domain = project
  .dependsOn(util)
  .configs(AccTest)
  .settings(
    name := "bujo-domain",
    inConfig(AccTest)(Defaults.testSettings ++ BloopDefaults.configSettings),
    libraryDependencies ++= Seq(
      scalatest % Test,
      scalatest % AccTest,
    ),
    Test / accTest := (AccTest / test).value,
    Test / jacocoReportSettings := JacocoReportSettings()
      .withFormats(JacocoReportFormats.ScalaHTML),
  )

lazy val repo = (project in file("repository"))
  .dependsOn(domain, schemas)
  .aggregate(schemas)
  .enablePlugins(SchemaUpdaterPlugin)
  .settings(
    name := "bujo-repository",
    scalaVersion := "2.13.5",
    libraryDependencies ++= Seq(
      "com.typesafe.slick"    %% "slick"          % "3.3.3",
      "org.slf4j"              % "slf4j-nop"      % "1.6.4",
      "com.typesafe.slick"    %% "slick-hikaricp" % "3.3.3",
      "com.github.pureconfig" %% "pureconfig"     % "0.15.0",
    ),
    libraryDependencies ++= Seq(scalatest, "com.h2database" % "h2" % "1.4.200")
      .map(_ % Test),
  )

lazy val schemasDeps = Seq(
  sqliteJdbc,
  slick_codegen,
  "org.slf4j"      % "slf4j-nop" % "2.0.0-alpha1",
  "com.h2database" % "h2"        % "1.4.200" % Test,
)

lazy val updateSchemas = taskKey[xsbti.compile.CompileAnalysis](
  "Migrates database, generates schemas and compiles.",
)
lazy val schemas = (project in file("repository/schemas"))
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "bujo-schemas",
    scalaVersion := "2.13.5",
    Compile / projectConfig := loadConfig[SchemasConfig](
      (Compile / resourceDirectory).value / "codegen.conf",
    ),
    Test / projectConfig := loadConfig[SchemasConfig](
      (Test / resourceDirectory).value / "codegen.conf",
    ),
    // flywayMigrate depends on flywayClasspath which is dynamic task that
    // triggers compile (via fullClasspath) if any of flywayLocations entries
    // is not a filesystem entry
    // (https://github.com/flyway/flyway-sbt/issues/10)
    flywayLocations := Seq(
      s"filesystem:${(baseDirectory.value / "migrations").getPath}",
    ),
    Test / flywayLocations := flywayLocations.value,
    flywayUrl := (Compile / projectConfig).value
      .asInstanceOf[SchemasConfig]
      .db
      .url,
    (Test / flywayUrl) := (Test / projectConfig).value
      .asInstanceOf[SchemasConfig]
      .db
      .url,
    libraryDependencies ++= schemasDeps,
    (Compile / sourceGenerators) += Def.task {
      val cp        = (Compile / dependencyClasspath).value
      val outputDir = (Compile / sourceManaged).value
      val config =
        (Compile / projectConfig).value.asInstanceOf[SchemasConfig]
      runner.value
        .run(
          "slick.codegen.SourceCodeGenerator",
          cp.files,
          Seq(
            config.slick.profile,
            config.db.driver,
            config.db.url,
            outputDir.getPath,
            config.slick.codegen.`package`,
          ),
          streams.value.log,
        )
        .get
      Seq(
        outputDir / config.slick.codegen.`package`
          .replace('.', '/') / "Tables.scala",
      )
    },
    (Test / sourceGenerators) += Def.task {
      val cp        = (Test / dependencyClasspath).value
      val outputDir = (Test / sourceManaged).value
      val config =
        (Test / projectConfig).value.asInstanceOf[SchemasConfig]
      runner.value
        .run(
          "slick.codegen.SourceCodeGenerator",
          cp.files,
          Seq(
            config.slick.profile,
            config.db.driver,
            config.db.url,
            outputDir.getPath,
            config.slick.codegen.`package`,
          ),
          streams.value.log,
        )
        .get
      Seq(
        outputDir / config.slick.codegen.`package`
          .replace('.', '/') / "Tables.scala",
      )
    },
    Compile / updateSchemas := Def
      .sequential(
        Compile / flywayMigrate,
        Compile / compile,
      )
      .value,
    Test / updateSchemas := Def
      .sequential(
        Test / flywayMigrate,
        Test / compile,
      )
      .value,
  )
