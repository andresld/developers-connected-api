import sbt.ModuleID

val compilerOptions: Seq[String] =
  Seq(
    "-language:higherKinds",
    "-language:implicitConversions",
    "-feature",
    "-deprecation",
    "-unchecked"
  )

val libDependencies: Seq[ModuleID] =
  Seq(
    dependencies.circe.generic,
    dependencies.circe.parser,
    dependencies.http4s.circe,
    dependencies.http4s.client,
    dependencies.http4s.dsl,
    dependencies.http4s.server,
    dependencies.log.binding,
    dependencies.log.slf4j,
    dependencies.test.core
  )

lazy val root = (project in file("."))
  .settings(
    organization         := "com.github.aldtid",
    name                 := "developers-connected-api",
    scalaVersion         := "2.13.6",
    version              := "0.1.0-SNAPSHOT",
    scalacOptions       ++= compilerOptions,
    libraryDependencies ++= libDependencies
  )
