package com.github.aldtid.developers.connected

import cats.effect.{IO, IOApp, ExitCode}
import org.http4s.dsl.io
import org.http4s.dsl.Http4sDsl

import scala.concurrent.ExecutionContext.global


object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    
    implicit val dsl: Http4sDsl[IO] = io
    
    launcher.start[IO](global)

  }

}
