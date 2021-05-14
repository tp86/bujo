ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.0.0-RC2"

lazy val scalatest = "org.scalatest" %% "scalatest" % "3.2.7"

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
  .settings(
    name := "bujo-domain",
    libraryDependencies += scalatest % Test,
  )

lazy val repo = (project in file("repository"))
  .dependsOn(domain)
  .settings(
    name := "bujo-repository",
  )
