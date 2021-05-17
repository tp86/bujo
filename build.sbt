import pureconfig.generic.auto._
import ProjectConfig._
import bloop.integrations.sbt.BloopDefaults

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
  .settings(
    name := "bujo-repository",
  )

lazy val schemasDeps = Seq(
  sqliteJdbc,
  slick_codegen,
  "org.slf4j" % "slf4j-nop" % "2.0.0-alpha1",
)

lazy val schemas = (project in file("repository/schemas"))
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "bujo-schemas",
    scalaVersion := "2.13.5",
    Compile / projectConfig := loadConfig[SchemasConfig](
      (Compile / resourceDirectory).value / "codegen.conf"
    ),
    flywayUrl := (Compile / projectConfig).value.asInstanceOf[SchemasConfig].db.url,
    libraryDependencies ++= schemasDeps,
    compile := Def
      .sequential(
        flywayMigrate,
        Def.task {
          val cp        = (Compile / dependencyClasspath).value
          val outputDir = (Compile / sourceManaged).value
          val config    = (Compile / projectConfig).value.asInstanceOf[SchemasConfig]
          runner.value
            .run(
              "slick.codegen.SourceCodeGenerator",
              cp.files,
              Seq(
                config.slick.profile,
                config.db.jdbcDriver,
                config.db.url,
                outputDir.getPath,
                config.slick.codegen.`package`,
              ),
              streams.value.log,
            )
            .get
          Seq(
            outputDir / config.slick.codegen.`package`.replace('.', '/') / "Tables.scala",
          )
        },
        Compile / compile,
      )
      .value,
  )
