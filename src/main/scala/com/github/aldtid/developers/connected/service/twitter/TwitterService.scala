package com.github.aldtid.developers.connected.service.twitter

import com.github.aldtid.developers.connected.logging.{Log, ProgramLog}
import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages._
import com.github.aldtid.developers.connected.logging.tags.twitterTag
import com.github.aldtid.developers.connected.service.util
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.service.twitter.error._
import com.github.aldtid.developers.connected.service.twitter.response.{Followers, UserData}

import cats.data.EitherT
import cats.effect.{Clock, Concurrent}
import cats.implicits._
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._
import org.http4s.AuthScheme.Bearer
import org.http4s.Credentials.Token
import org.http4s.{Headers, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.typelevel.log4cats.Logger


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
   * Request, response and function result are logged.
   *
   * @param username Twitter username to retrieve the user information for
   * @param C Clock instance
   * @param F Concurrent instance
   * @param client http client
   * @param logger logger instance
   * @param connection connection information to perform the requests
   * @tparam L type to format the logs
   * @return the user information for related username if it exists or an error otherwise
   */
  def getUserByUsername[L : ProgramLog](username: String)
                                       (implicit F: Concurrent[F],
                                        C: Clock[F],
                                        client: Client[F],
                                        logger: Logger[F],
                                        connection: TwitterConnection): EitherT[F, Error, UserData]

  /**
   * Retrieves the followers for passed identifier.
   *
   * In case an error happens during the retrieve, then it is returned as a Left. Only in case the
   * returned user option is not empty it is possible to assure the existence of the user.
   *
   * Request, response and function result are logged.
   *
   * @param id Twitter identifier to retrieve the followers for
   * @param F Concurrent instance
   * @param C Clock instance
   * @param client http client
   * @param logger logger instance
   * @param connection connection information to perform the requests
   * @tparam L type to format the logs
   * @return the user followers for related identifier or an error otherwise
   */
  def getUserFollowers[L : ProgramLog](id: String)
                                      (implicit F: Concurrent[F],
                                       C: Clock[F],
                                       client: Client[F],
                                       logger: Logger[F],
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
   * @tparam L type to format the logs
   * @return the user if it exists or an error otherwise
   */
  def getUserByUsername[F[_] : Concurrent : Clock : Logger, L](username: String)
                                                              (implicit client: Client[F],
                                                               pl: ProgramLog[L],
                                                               connection: TwitterConnection): F[Either[Error, UserData]] = {

    import pl._

    val uri: Uri = connection.baseUri / "users" / "by" / "username" / username
    val request: Request[F] = Request(uri = uri, headers = Headers(authHeader))
    val baseLog: Log[L] = username.asUsername |+| twitterTag

    val handle: Response[F] => F[Either[Error, UserData]] = {
      case Status.Successful(response)   => bodyAs[F, UserData](response).map(identity)
      case Status.BadRequest(response)   => util.bodyAs(response, (_, body) => Left(BadRequest(body)))
      case Status.Unauthorized(response) => util.bodyAs(response, (_, body) => Left(Unauthorized(body)))
      case response                      => util.bodyAs(response, (s, b) => Left(UnexpectedResponse(s, b, None)))
    }

    util.requestWithLogs(request, baseLog |+| twitterUserRequest, baseLog |+| twitterUserResponse, handle)
      .flatTap(util.logResult(baseLog |+| twitterUserSuccess, baseLog |+| twitterUserError))

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
   * @tparam L type to format the logs
   * @return the user followers or an error otherwise
   */
  def getUserFollowers[F[_] : Concurrent : Clock : Logger, L](id: String)
                                                             (implicit client: Client[F],
                                                              pl: ProgramLog[L],
                                                              connection: TwitterConnection): F[Either[Error, Followers]] = {

    import pl._

    val uri: Uri = connection.baseUri / "users" / id / "followers"
    val request: Request[F] = Request(uri = uri, headers = Headers(authHeader))
    val baseLog: Log[L] = id.asIdentifier |+| twitterTag

    val handle: Response[F] => F[Either[Error, Followers]] = {
      case Status.Successful(response) => bodyAs[F, Followers](response).map(identity)
      case response                    => util.bodyAs(response, (s, b) => Left(UnexpectedResponse(s, b, None)))
    }

    util.requestWithLogs(request, baseLog |+| twitterFollowersRequest, baseLog |+| twitterFollowersResponse, handle)
      .flatTap(util.logResult(baseLog |+| twitterFollowersSuccess, baseLog |+| twitterFollowersError))

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
    util.bodyAs(response, (status, body, error) => UnexpectedResponse(status, body, Some(error)))

  /**
   * Creates a default implementation for the service using an F type as context.
   *
   * @tparam F context type
   * @return the default implementation for this service
   */
  def default[F[_]]: TwitterService[F] = new TwitterService[F] {

    def getUserByUsername[L : ProgramLog](username: String)
                                         (implicit F: Concurrent[F],
                                          C: Clock[F],
                                          client: Client[F],
                                          logger: Logger[F],
                                          connection: TwitterConnection): EitherT[F, Error, UserData] =
      EitherT(TwitterService.getUserByUsername(username))

    def getUserFollowers[L : ProgramLog](id: String)
                                        (implicit F: Concurrent[F],
                                         C: Clock[F],
                                         client: Client[F],
                                         logger: Logger[F],
                                         connection: TwitterConnection): EitherT[F, Error, Followers] =
      EitherT(TwitterService.getUserFollowers(id))

  }

}
