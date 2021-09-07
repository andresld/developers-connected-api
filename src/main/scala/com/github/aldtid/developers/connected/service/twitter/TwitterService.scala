package com.github.aldtid.developers.connected.service.twitter

import com.github.aldtid.developers.connected.service.utils
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.service.twitter.error._
import com.github.aldtid.developers.connected.service.twitter.response.{Followers, UserData}

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits._
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.http4s.AuthScheme.Bearer
import org.http4s.Credentials.Token
import org.http4s.{Headers, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.headers.Authorization

/**
 * Defines a service to interact with [[https://dev.twitter.com/rest/public Twitter API]].
 *
 * The interactions with the API are defined by service functions. They all permit to return an error using [[EitherT]]
 * instances, preventing to rely on [[Throwable]] or [[cats.MonadError]] instances. Received json responses from the
 * API are decoded into specific model instances, making them easier to work with.
 *
 * @tparam F context type
 */
trait TwitterService[F[_]] {

  /**
   * Retrieves the user information related with passed username.
   *
   * In case an error happens during the retrieve, then it is returned as a Left. If the returned response is not an
   * error that means that passed user exists.
   *
   * @param username Twitter username to retrieve the user information for
   * @param F Concurrent instance
   * @param client http client
   * @param connection connection information to perform the requests
   * @return the user information for related username if it exists or an error otherwise
   */
  def getUserByUsername(username: String)
                       (implicit F: Concurrent[F],
                        client: Client[F],
                        connection: TwitterConnection): EitherT[F, Error, UserData]

  /**
   * Retrieves the followers for passed identifier.
   *
   * In case an error happens during the retrieve, then it is returned as a Left. If the returned response is not an
   * error that means that passed user exists.
   *
   * @param id Twitter identifier to retrieve the followers for
   * @param F Concurrent instance
   * @param client http client
   * @param connection connection information to perform the requests
   * @return the user followers for related identifier or an error otherwise
   */
  def getUserFollowers(id: String)
                      (implicit F: Concurrent[F],
                       client: Client[F],
                       connection: TwitterConnection): EitherT[F, Error, Followers]

}

object TwitterService {

  /**
   * Implicit decoder configuration to decode json fields using a snake-case naming strategy.
   */
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  /**
   * User retrieve by username default implementation.
   *
   * @param username Twitter username to retrieve the user for
   * @param client http client
   * @param connection connection information to perform the requests
   * @tparam F context type
   * @return the user if it exists or an error otherwise
   */
  def getUserByUsername[F[_] : Concurrent](username: String)
                                          (implicit client: Client[F],
                                           connection: TwitterConnection): EitherT[F, Error, UserData] = {

    val uri: Uri = connection.baseUri / "users" / "by" / "username" / username
    val request: Request[F] = Request(uri = uri, headers = Headers(authHeader))

    EitherT(client.run(request).use({
      case Status.Successful(response) => bodyAs[F, UserData](response).map(identity)
      case Status.NotFound(response)   => utils.bodyAs(response, (_, body) => Left(NotFound(body)))
      case response                    => utils.bodyAs(response, (s, b) => Left(UnexpectedResponse(s, b, None)))
    }))

  }

  /**
   * User followers retrieve by username default implementation.
   *
   * In case no followers are found for a user, a None will be returned for [[Followers.data]].
   *
   * @param id Twitter identifier to retrieve the followers for
   * @param client http client
   * @param connection connection information to perform the requests
   * @tparam F context type
   * @return the user followers or an error otherwise
   */
  def getUserFollowers[F[_] : Concurrent](id: String)
                                         (implicit client: Client[F],
                                          connection: TwitterConnection): EitherT[F, Error, Followers] = {

    val uri: Uri = connection.baseUri / "users" / id / "followers"
    val request: Request[F] = Request(uri = uri, headers = Headers(authHeader))

    EitherT(client.run(request).use({
      case Status.Successful(response) => bodyAs[F, Followers](response).map(identity)
      case response                    => utils.bodyAs(response, (s, b) => Left(UnexpectedResponse(s, b, None)))
    }))

  }

  /**
   * Authorization header to perform the requests to the API.
   *
   * @param connection connection information to perform the requests
   * @return the authorization header
   */
  def authHeader(implicit connection: TwitterConnection): Authorization =
    Authorization(Token(Bearer, connection.token))

  /**
   * Utility function to decode a json response body.
   *
   * In case the json cannot be parsed, an [[UnexpectedResponse]] error will be returned.
   *
   * @param response http response
   * @tparam F context type
   * @tparam A type to decode the body to
   * @return the body decoded as an instance of A or an error otherwise
   */
  def bodyAs[F[_] : Concurrent, A : Decoder](response: Response[F]): F[Either[UnexpectedResponse, A]] =
    utils.bodyAs(response, (status, body, error) => UnexpectedResponse(status, body, Some(error)))

  /**
   * Creates a default implementation for the service using an F type as context.
   *
   * @tparam F context type
   * @return the default implementation for this service
   */
  def default[F[_]]: TwitterService[F] = new TwitterService[F] {

    def getUserByUsername(username: String)
                         (implicit F: Concurrent[F],
                          client: Client[F],
                          connection: TwitterConnection): EitherT[F, Error, UserData] =
      TwitterService.getUserByUsername(username)

    def getUserFollowers(id: String)
                        (implicit F: Concurrent[F],
                         client: Client[F],
                         connection: TwitterConnection): EitherT[F, Error, Followers] =
      TwitterService.getUserFollowers(id)

  }

}
