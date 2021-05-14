ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.0.0-RC3"

lazy val bujo = (project in file("."))
  .aggregate(domain)
  .settings(
    name := "bujo",
  )

lazy val domain = project
  .settings(
    name := "bujo-domain",
  )

lazy val repo = (project in file("repository"))
  .dependsOn(domain)
  .settings(
    name := "bujo-repository",
  )
