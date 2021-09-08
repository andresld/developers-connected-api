package com.github.aldtid.developers.connected.service.github

import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages._
import com.github.aldtid.developers.connected.logging.tags.githubTag
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.response._
import com.github.aldtid.developers.connected.service.github.error._
import com.github.aldtid.developers.connected.service.util

import cats.data.EitherT
import cats.effect.Concurrent
import cats.effect.kernel.Clock
import cats.implicits._
import com.github.aldtid.developers.connected.logging.{Log, ProgramLog}
import io.circe.Decoder
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration
import org.http4s.{BasicCredentials, Headers, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.typelevel.log4cats.Logger


/**
 * Defines a service to interact with [[https://docs.github.com/en/rest GitHub API]].
 *
 * The interactions with the API are defined by service functions. They all permit to return an error using [[EitherT]]
 * instances, preventing to rely on [[Throwable]] or [[cats.MonadError]] instances. Received json responses from the
 * API are decoded into specific model instances, making them easier to work with.
 *
 * @tparam F context type
 */
trait GitHubService[F[_]] {

  /**
   * Retrieves the organizations, if any, for passed user.
   *
   * In case an error happens during the retrieve, then it is returned as a Left. If the returned response is not an
   * error that means that passed user exists.
   *
   * Request, response and function result are logged.
   *
   * @param username GitHub username to retrieve organizations for
   * @param F Concurrent instance
   * @param C Clock instance
   * @param client http client
   * @param logger logger instance
   * @param connection connection information to perform the requests
   * @return the user organizations list if it exists or an error otherwise
   */
  def getOrganizations[L : ProgramLog](username: String)
                                      (implicit F: Concurrent[F],
                                       C: Clock[F],
                                       client: Client[F],
                                       logger: Logger[F],
                                       connection: GitHubConnection): EitherT[F, Error, List[Organization]]

}

object GitHubService {

  /**
   * Implicit decoder configuration to decode json fields using a snake-case naming strategy.
   */
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  /**
   * Organizations retrieve default implementation.
   *
   * @param username GitHub username to retrieve organizations for
   * @param client http client
   * @param connection connection information to perform the requests
   * @tparam F context type
   * @return the user organizations list if it exists or an error otherwise
   */
  def getOrganizations[F[_] : Concurrent : Clock: Logger, L](username: String)
                                                            (implicit client: Client[F],
                                                             pl: ProgramLog[L],
                                                             connection: GitHubConnection): F[Either[Error, List[Organization]]] = {

    import pl._

    val uri: Uri = connection.baseUri / "users" / username / "orgs"
    val headers: Headers = Headers(Authorization(BasicCredentials(connection.username, connection.token)))
    val request: Request[F] = Request(uri = uri, headers = headers)
    val baseLog: Log[L] = username.asUsername |+| githubTag

    val handle: Response[F] => F[Either[Error, List[Organization]]] = {
      case Status.Successful(response) => bodyAs[F, List[Organization]](response).map(identity)
      case Status.NotFound(response)   => util.bodyAs(response, (_, body) => Left(NotFound(body)))
      case response                    => util.bodyAs(response, (s, b) => Left(UnexpectedResponse(s, b, None)))
    }

    util.requestWithLogs(request, baseLog |+| githubOrganizationsRequest, baseLog |+| githubOrganizationsResponse, handle)
      .flatTap(util.logResult(baseLog |+| githubOrganizationsSuccess, baseLog |+| githubOrganizationsError))

  }

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
  def default[F[_]]: GitHubService[F] = new GitHubService[F] {

    def getOrganizations[L : ProgramLog](username: String)
                                        (implicit F: Concurrent[F],
                                         C: Clock[F],
                                         client: Client[F],
                                         logger: Logger[F],
                                         connection: GitHubConnection): EitherT[F, Error, List[Organization]] =
      EitherT(GitHubService.getOrganizations(username))

  }

}
