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

lazy val dbConfig = Map(
  "dbUrl"       -> "jdbc:sqlite:db/bujo.db",
  "slickDriver" -> "slick.jdbc.SQLiteProfile",
  "jdbcDriver"  -> "org.sqlite.JDBC",
  "slickPkg"    -> "bujo.repository.schemas",
)

lazy val slickCodegen =
  taskKey[Seq[File]]("Generate schema model based on database.")

lazy val schemas = (project in file("repository/schemas"))
  .enablePlugins(FlywayPlugin)
  .settings(
    name := "bujo-schemas",
    scalaVersion := "2.13.5",
    flywayUrl := dbConfig("dbUrl"),
    libraryDependencies ++= schemasDeps,
    slickCodegen := {
      val cp        = (Compile / dependencyClasspath).value
      val outputDir = (Compile / sourceManaged).value
      runner.value
        .run(
          "slick.codegen.SourceCodeGenerator",
          cp.files,
          Array(
            dbConfig("slickDriver"),
            dbConfig("jdbcDriver"),
            dbConfig("dbUrl"),
            outputDir.getPath,
            dbConfig("slickPkg"),
          ),
          streams.value.log,
        )
        .get
      Seq(outputDir / dbConfig("slickPkg").replace('.', '/') / "Tables.scala")
    },
    Compile / sourceGenerators += slickCodegen.taskValue,
    compile := Def.taskDyn {
      val comp = (Compile / compile).value
      Def.task {
        flywayMigrate.value
        comp
      }
    }.value,
  )
