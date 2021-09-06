package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.handler.DevelopersHandler
import com.github.aldtid.developers.connected.logging.ProgramLog
import com.github.aldtid.developers.connected.encoder.BodyEncoder
import cats.effect.ExitCode
import cats.effect.kernel.Async
import cats.implicits._
import com.github.aldtid.developers.connected.service.github.GitHubService
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext


object launcher {

  def prepareAndStart[F[_] : Async : Http4sDsl : Logger, L : ProgramLog, O : BodyEncoder](serverEC: ExecutionContext,
                                                                                          clientEC: ExecutionContext): F[ExitCode] =
    BlazeClientBuilder[F](clientEC).resource.use(implicit client => {

      implicit val ghConnection: GitHubConnection = GitHubConnection(Uri(), "username", "token")

      start[F, L, O](serverEC, GitHubService.default)

    })

  def start[F[_] : Async : Http4sDsl : Client : Logger, L : ProgramLog, O : BodyEncoder](ec: ExecutionContext,
                                                                                         github: GitHubService[F])
                                                                                        (implicit ghConnection: GitHubConnection): F[ExitCode] =

    BlazeServerBuilder[F](ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(application.app(DevelopersHandler.default(github)))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
