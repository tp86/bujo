ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.0.0"

lazy val bujo = (project in file("."))
  .settings(
    name := "bujo",
  )

lazy val domain = project
  .settings(
    name := "bujo-domain",
  )
