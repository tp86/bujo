import SchemaUpdater_._
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
  "org.slf4j"      % "slf4j-nop" % "2.0.0-alpha1",
  "com.h2database" % "h2"        % "1.4.200" % Test,
)

// lazy val schemaGenerator = taskKey[Seq[File]](
  // "Generates schemas based on database.",
// )

lazy val schemas = (project in file("repository/schemas"))
  .enablePlugins(SchemaUpdater)
  .settings(
    name := "bujo-schemas",
    scalaVersion := "2.13.5",
    // flywayMigrate depends on flywayClasspath which is dynamic task that
    // triggers compile (via fullClasspath) if any of flywayLocations entries
    // is a classpath entry
    // (https://github.com/flyway/flyway-sbt/issues/10)
    flywayLocations := Seq(
      s"filesystem:${(baseDirectory.value / "migrations").getPath}",
    ),
    Test / flywayLocations := flywayLocations.value,
    flywayUrl := s"""jdbc:sqlite:${(ThisBuild / baseDirectory).value / "db/bujo.db"}""",
    Test / flywayUrl := "jdbc:h2:mem:test",
    libraryDependencies ++= schemasDeps,
    Compile / schemaUpdateConfig_ := SchemaUpdaterConfig(
      flywayUrl.value,
      "org.sqlite.JDBC",
      "slick.jdbc.SQLiteProfile",
      "bujo.repository.schemas",
    ),
    Compile / schemaUpdate_ := {
      generateSchema(
        (Compile / schemaUpdateConfig_).value,
        (Compile / dependencyClasspath).value,
        (Compile / sourceManaged).value,
        runner.value,
        streams.value.log,
      )
    },
  )
