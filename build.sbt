import bloop.integrations.sbt.BloopDefaults

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.0.0-RC2"

lazy val AccTest = config("acceptance-test") extend (Test)
lazy val accTest = taskKey[Unit]("Executes acceptance tests.")

lazy val scalatest  = "org.scalatest" %% "scalatest"   % "3.2.7"
lazy val sqliteJdbc = "org.xerial"     % "sqlite-jdbc" % "3.34.0"

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
  .settings(
    name := "bujo-repository",
  )

lazy val schemasDeps = Seq(
  sqliteJdbc,
)

lazy val schemas = (project in file("repository/schemas"))
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "bujo-schemas",
    flywayUrl := "jdbc:sqlite:db/bujo.db",
    libraryDependencies ++= schemasDeps,
    compile := Def.taskDyn {
      val comp = (Compile / compile).value
      Def.task {
        flywayMigrate.value
        comp
      }
    }.value,
  )
