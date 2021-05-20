libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.15.0"

addSbtPlugin("io.github.davidmweber" % "flyway-sbt"   % "7.4.0")

lazy val schemaUpdaterPlugin = (project in file("sbt-schemaupdater"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-schemaupdater",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.4.8"
      }
    },
  )
