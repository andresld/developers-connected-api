import sbt.ModuleID

val compilerOptions: Seq[String] =
  Seq(
    "-language:higherKinds",
    "-feature",
    "-unchecked"
  )

val libDependencies: Seq[ModuleID] =
  Seq(
    dependencies.http4s.dsl,
    dependencies.http4s.server,
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
