lazy val root = (project in file("."))
  .settings()

lazy val schemaUpdater = (project in file("schema_updater"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-schemaupdater",
    version := "0.2.4",
    versionScheme := Some("semver-spec"),
    organization := "bujo",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.4.8"
      }
    },
    addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "7.4.0"),
  )
