import bloop.integrations.sbt.BloopDefaults

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.0.0-RC2"

lazy val AccTest = config("acceptance-test") extend (Test)
lazy val accTest = taskKey[Unit]("Executes acceptance tests.")

lazy val scalatest  = "org.scalatest" %% "scalatest"   % "3.2.7"
lazy val sqliteJdbc = "org.xerial"     % "sqlite-jdbc" % "3.34.0"
lazy val h2         = "com.h2database" % "h2"          % "1.4.200"

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
  h2 % Test,
)

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
    Compile / flywayUrl := s"""jdbc:sqlite:${(ThisBuild / baseDirectory).value / "db/bujo.db"}""",
    Compile / schemaUpdateDbProfile := SchemaUpdater.SqliteProfile,
    Test / flywayUrl := "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE",
    Test / schemaUpdateDbProfile := SchemaUpdater.H2Profile,
    libraryDependencies ++= schemasDeps,
    schemaUpdateOutputPackage := "bujo.repository.schema",
  )
