package com.github.aldtid.developers.connected.service

import cats.effect.Concurrent
import cats.implicits._
import io.circe.{Decoder, Error => CError}
import io.circe.parser.decode
import org.http4s.Response


object utils {

  /**
   * Utility function to decode a json response body.
   *
   * In case the json cannot be parsed, an E error will be returned.
   *
   * @param response http response
   * @param onError
   * @tparam F context type
   * @tparam A type to decode the body to
   * @return the body decoded as an instance of A or an error otherwise
   */
  def bodyAs[F[_] : Concurrent, E, A : Decoder](response: Response[F],
                                                onError: (Int, String, CError) => E): F[Either[E, A]] =
    response.as[String].map(body => decode(body).leftMap(onError(response.status.code, body, _)))

  /**
   *
   * @param response
   * @param f
   * @tparam F
   * @tparam A
   * @return
   */
  def bodyAs[F[_] : Concurrent, A](response: Response[F], f: (Int, String) => A): F[A] =
    response.as[String].map(body => f(response.status.code, body))

}
