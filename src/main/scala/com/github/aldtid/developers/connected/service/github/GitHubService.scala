package com.github.aldtid.developers.connected.service.github

import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.response._
import com.github.aldtid.developers.connected.service.github.error._

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.parser.decode
import org.http4s.{BasicCredentials, Headers, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.http4s.headers.Authorization


trait GitHubService[F[_]] {

  def getOrganizations(developer: String)
                      (implicit F: Concurrent[F],
                       client: Client[F],
                       connection: GitHubConnection): EitherT[F, Error, List[Organization]]

}

object GitHubService {

  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  def getOrganizations[F[_] : Concurrent](developer: String)
                                         (implicit client: Client[F],
                                          connection: GitHubConnection): EitherT[F, Error, List[Organization]] = {

    val uri: Uri = connection.baseUri / "users" / developer / "orgs"
    val headers: Headers = Headers(Authorization(BasicCredentials(connection.username, connection.token)))
    val request: Request[F] = Request(uri = uri, headers = headers)

    EitherT(client.run(request).use({
      case Status.Successful(response) => bodyAs[F, List[Organization]](response)
      case response                    => bodyAs[F, DefaultError](response).map(_.flatMap(Left(_)))
    }))

  }

  def bodyAs[F[_] : Concurrent, A : Decoder](response: Response[F]): F[Either[Error, A]] =
    response.as[String].map(body => decode(body).leftMap(UnexpectedResponse(response.status.code, body, _)))

  def default[F[_]]: GitHubService[F] = new GitHubService[F] {

    def getOrganizations(developer: String)
                        (implicit F: Concurrent[F],
                         client: Client[F],
                         connection: GitHubConnection): EitherT[F, Error, List[Organization]] =
      GitHubService.getOrganizations(developer)

  }

}
