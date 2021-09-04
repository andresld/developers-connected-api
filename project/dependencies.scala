import sbt._


object dependencies {

  lazy val version = new {
    val http4s: String = "0.23.1"
  }

  lazy val http4s = new {
    val dsl: ModuleID = "org.http4s" %% "http4s-dsl" % version.http4s
    val server: ModuleID = "org.http4s" %% "http4s-blaze-server" % version.http4s
  }

  lazy val test = new  {
    val core: ModuleID = "org.scalatest" %% "scalatest" % "3.1.1" % Test
  }
  
}
