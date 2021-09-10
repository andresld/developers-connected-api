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
    dependencies.pureconfig.config,
    dependencies.test.core
  )

lazy val root = (project in file("."))
  .settings(
    // Base definitions
    organization         := "com.github.aldtid",
    name                 := "developers-connected-api",
    scalaVersion         := "2.13.6",
    version              := "0.1.0-SNAPSHOT",
    scalacOptions       ++= compilerOptions,
    libraryDependencies ++= libDependencies,
    // Docker definitions
    Docker / packageName                 := "developers-connected-api",
    Docker / defaultLinuxInstallLocation := "/opt/developers-connected-api",
    dockerBaseImage                      := "openjdk:11-jdk-slim",
    dockerLabels                         := Map("version" -> version.value),
    dockerExposedPorts                   += 8080,
    dockerExposedVolumes                 := Seq(
      "/opt/developers-connected-api/config",
      "/opt/developers-connected-api/log"
    ),
    // Plugins definitions
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
