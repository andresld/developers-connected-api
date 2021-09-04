package com.github.aldtid.developers.connected

import cats.effect.ExitCode
import cats.effect.kernel.Async
import cats.implicits._
import com.github.aldtid.developers.connected.logging.ProgramLog
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext


object launcher {

  def start[F[_] : Async : Http4sDsl : Logger, L : ProgramLog](ec: ExecutionContext): F[ExitCode] =

    BlazeServerBuilder[F](ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(service.app)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
