package com.github.aldtid.developers.connected

import cats.effect.ExitCode
import cats.effect.kernel.Async
import cats.implicits._
import com.github.aldtid.developers.connected.controller.DevelopersController
import com.github.aldtid.developers.connected.logging.ProgramLog
import com.github.aldtid.developers.connected.model.encoder.ResponseEncoder
import org.http4s.EntityEncoder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext


object launcher {

  def start[F[_] : Async : Http4sDsl : Logger, L : ProgramLog, O : ResponseEncoder](ec: ExecutionContext)
                                                                                   (implicit encoder: EntityEncoder[F, O]): F[ExitCode] =

    BlazeServerBuilder[F](ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(application.app(DevelopersController.default[F]))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
