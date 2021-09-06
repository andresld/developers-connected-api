import sbt._


object dependencies {

  lazy val version = new {
    val circe: String = "0.14.1"
    val http4s: String = "0.23.1"
    val logback: String = "1.2.5"
    val scalatest: String = "3.2.9"
    val slf4j: String = "2.1.1"
  }

  lazy val circe = new {
    val generic: ModuleID = "io.circe" %% "circe-generic-extras" % version.circe
    val parser: ModuleID = "io.circe" %% "circe-parser" % version.circe
  }

  lazy val http4s = new {
    val circe: ModuleID = "org.http4s" %% "http4s-circe" % version.http4s
    val client: ModuleID = "org.http4s" %% "http4s-blaze-client" % version.http4s
    val dsl: ModuleID = "org.http4s" %% "http4s-dsl" % version.http4s
    val server: ModuleID = "org.http4s" %% "http4s-blaze-server" % version.http4s
  }

  lazy val log = new {
    val slf4j: ModuleID = "org.typelevel" %% "log4cats-slf4j" % version.slf4j
    val binding: ModuleID = "ch.qos.logback" % "logback-classic" % version.logback
  }

  lazy val test = new  {
    val core: ModuleID = "org.scalatest" %% "scalatest" % version.scalatest % Test
  }
  
}
