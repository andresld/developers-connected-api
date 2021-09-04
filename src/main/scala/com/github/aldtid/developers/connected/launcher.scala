package com.github.aldtid.developers.connected

import cats.Monad
import cats.effect.ExitCode
import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.{HttpRoutes, HttpApp}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._

import scala.concurrent.ExecutionContext


object launcher {
  
  def service[F[_] : Monad](implicit dsl: Http4sDsl[F]): HttpApp[F] = {

    import dsl._

    HttpRoutes.of[F] {

      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")

    }.orNotFound

  }

  def start[F[_] : Async : Http4sDsl](ec: ExecutionContext): F[ExitCode] =

    BlazeServerBuilder[F](ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(service)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
