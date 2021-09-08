package com.github.aldtid.developers.connected.service

import com.github.aldtid.developers.connected.logging.{Log, Loggable, ProgramLog}
import com.github.aldtid.developers.connected.logging.implicits.model._

import cats.effect.{Clock, Concurrent}
import cats.implicits._
import io.circe.{Decoder, Error => CError}
import io.circe.parser.decode
import org.http4s.{Request, Response}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration


object util {

  /**
   * Perform a request using an http client for which the request itself and the response are logged. Additional log
   * values are passed to enhance both logs. Once the response has been retrieved, a handler function is applied to it.
   *
   * @param request request to perform
   * @param requestLog logs to enhance request log
   * @param responseLog logs to enhance response log
   * @param handle function to apply to the response
   * @param client http client
   * @param logger logger instance
   * @param pl logging instances
   * @tparam F context type
   * @tparam L logging type to format
   * @tparam O output type
   * @return the result of applying the handler function to retrieved response
   */
  def requestWithLogs[F[_] : Concurrent : Clock, L, O](request: Request[F],
                                                       requestLog: Log[L],
                                                       responseLog: Log[L],
                                                       handle: Response[F] => F[O])
                                                      (implicit client: Client[F],
                                                      logger: Logger[F],
                                                      pl: ProgramLog[L]): F[O] = {

    import pl._

    def onResponse(response: Response[F], start: FiniteDuration): F[O] =
      for {
        end     <- Clock[F].realTime
        latency  = (end - start).toMillis.asLatency
        _       <- Logger[F].debug(responseLog |+| response |+| latency)
        o       <- handle(response)
      } yield o

    for {
      start <- Clock[F].realTime
      _     <- Logger[F].debug(requestLog |+| request)
      o     <- client.run(request).use(onResponse(_, start))
    } yield o

  }

  /**
   * Creates a function that logs an either with two different logs depending on if the either is a Right or a Left.
   *
   * @param successLog log to apply if the either is a Right
   * @param errorLog log to apply if the either is a Left
   * @param logger logger instance
   * @param la logging instance for a Right value
   * @param le logging instance for a Left value
   * @tparam F context type
   * @tparam L logging type to format
   * @tparam E error type
   * @tparam A success type
   * @return a function that takes an either and perform different logs depending the case
   */
  def logResult[F[_], L, E, A](successLog: Log[L],
                               errorLog: Log[L])
                              (implicit logger: Logger[F],
                               la: Loggable[A, L],
                               le: Loggable[E, L]): Either[E, A] => F[Unit]  =
    _.fold(e => Logger[F].error(errorLog |+| e), a =>  Logger[F].debug(successLog |+| a))

  /**
   * Utility function to decode a json response body.
   *
   * In case the json cannot be parsed, an E error will be returned applying passed 'onError' function.
   *
   * @param response http response
   * @param onError function to apply in case the decoding fails
   * @tparam F context type
   * @tparam E error type
   * @tparam A type to decode the body to
   * @return the body decoded as an instance of A or an error otherwise
   */
  def bodyAs[F[_] : Concurrent, E, A : Decoder](response: Response[F],
                                                onError: (Int, String, CError) => E): F[Either[E, A]] =
    response.as[String].map(body => decode(body).leftMap(onError(response.status.code, body, _)))

  /**
   * Utility function to extract a body and transform it with passed function.
   *
   * @param response http response
   * @param f function to
   * @tparam F context type
   * @tparam A type to transform the body into
   * @return the transformed body into an A instance
   */
  def bodyAs[F[_] : Concurrent, A](response: Response[F], f: (Int, String) => A): F[A] =
    response.as[String].map(body => f(response.status.code, body))

}
