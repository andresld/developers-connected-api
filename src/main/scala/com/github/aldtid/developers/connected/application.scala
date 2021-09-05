package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.controller.DevelopersController
import com.github.aldtid.developers.connected.encoder.BodyEncoder
import com.github.aldtid.developers.connected.encoder.implicits._
import com.github.aldtid.developers.connected.logging.ProgramLog
import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages._
import com.github.aldtid.developers.connected.model.Developers
import com.github.aldtid.developers.connected.model.responses.{Errors, MissingResource}

import cats.Monad
import cats.effect.Sync
import cats.implicits._
import org.http4s.{HttpApp, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger


object application {

  def app[F[_] : Sync : Logger : Http4sDsl, L, O : BodyEncoder](controller: DevelopersController[F])
                                                               (implicit pl: ProgramLog[L]): HttpApp[F] = {

    import pl._

    HttpApp[F](request =>

      for {

        start    <- Sync[F].realTime
        _        <- Logger[F].info(incomingRequest |+| request)

        response <- process(request, controller)

        end      <- Sync[F].realTime
        latency   = (end - start).toMillis.asLatency
        _        <- Logger[F].info(outgoingResponse |+| response |+| latency)

      } yield response

    )

  }

  def process[F[_] : Monad, L : ProgramLog, O](request: Request[F],
                                               controller: DevelopersController[F])
                                              (implicit dsl: Http4sDsl[F],
                                               be: BodyEncoder[O]): F[Response[F]] = {

    import dsl._, be._

    request match {

      case GET -> Root / "developers" / "connected" / dev1 / dev2 =>
        controller.checkConnection(Developers(dev1, dev2))
          .leftMap(errors => BadRequest(Errors(errors).encode))
          .map(connection => Ok(connection.encode))
          .foldF(identity, identity)

      case _ =>
        NotFound(Errors.of(MissingResource).encode)

    }

  }

}
