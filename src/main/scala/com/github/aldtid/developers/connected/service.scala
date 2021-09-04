package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.logging.ProgramLog
import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages._

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import org.http4s.{HttpApp, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger


object service {

  def app[F[_] : Sync : Logger : Http4sDsl, L](implicit pl: ProgramLog[L]): HttpApp[F] = {

    import pl._

    HttpApp[F](request =>

      for {
        start    <- Sync[F].realTime
        _        <- Logger[F].info(incomingRequest |+| request)

        response <- process(request)

        end      <- Sync[F].realTime
        latency   = (end - start).toMillis.asLatency
        _        <- Logger[F].info(outgoingResponse |+| response |+| latency)
      } yield response

    )

  }

  def process[F[_] : Monad](request: Request[F])(implicit dsl: Http4sDsl[F]): F[Response[F]] = {

    import dsl._

    request match {
      case GET -> Root / "hello" / name => Ok(s"Hello, $name.")
      case _ => NotFound("Missing resource")
    }
  }

}
