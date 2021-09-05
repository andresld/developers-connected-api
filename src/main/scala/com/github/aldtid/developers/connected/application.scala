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

  /**
   * Main application routes instance.
   *
   * Composes all the application routes, logs the requests and responses and processes the incoming requests.
   *
   * @param controller 'developers' endpoints controller
   * @param pl logging instances
   * @tparam F context
   * @tparam L logging type to format
   * @tparam O body type to encode
   * @return
   */
  def app[F[_] : Sync : Logger : Http4sDsl, L, O : BodyEncoder](controller: DevelopersController[F])
                                                               (implicit pl: ProgramLog[L]): HttpApp[F] = {

    import pl._

    // Routes composition
    val process: Function[Request[F], F[Response[F]]] = developers(controller) applyOrElse (_, notFound)

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

  /**
   * Defines the routes for 'developers' endpoints group.
   *
   * All the endpoints defined are managed by passed [[DevelopersController]] instance. As a partial function, no other
   * that 'developers' endpoints will be managed by this function.
   *
   * @param controller controller functions for each endpoint
   * @param dsl routes dsl
   * @param be body encoder
   * @tparam F context
   * @tparam L logging type to format
   * @tparam O body type to encode
   * @return a partial function that handles each of 'developers' endpoints
   */
  def developers[F[_] : Monad, L : ProgramLog, O](controller: DevelopersController[F])
                                                 (implicit dsl: Http4sDsl[F],
                                                  be: BodyEncoder[O]): PartialFunction[Request[F], F[Response[F]]] = {

    import dsl._, be._

    {

      case GET -> Root / "developers" / "connected" / dev1 / dev2 =>
        controller.checkConnection(Developers(dev1, dev2))
          .leftMap(errors => BadRequest(Errors(errors).encode))
          .map(connection => Ok(connection.encode))
          .foldF(identity, identity)

    }

  }

  /**
   * Fallback function for when a route does not match with any of previous routes.
   *
   * This function replaces [[org.http4s.syntax.KleisliResponseOps.orNotFound]] syntax, defining a custom response for
   * routes that do not match with exposed API.
   *
   * @param dsl routes dsl
   * @param be body encoder
   * @tparam F context
   * @tparam O body type to encode
   * @return a function that always returns a not found error with a custom body
   */
  def notFound[F[_] : Monad, O](implicit dsl: Http4sDsl[F], be: BodyEncoder[O]): Request[F] => F[Response[F]] = {

    import dsl._, be._

    _ => NotFound(Errors.of(MissingResource).encode)

  }

}
