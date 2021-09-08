package com.github.aldtid.developers.connected.handler

import com.github.aldtid.developers.connected.model.Developers
import com.github.aldtid.developers.connected.model.responses.{Connected, Connection, Error, NotConnected}
import com.github.aldtid.developers.connected.service.github.GitHubService
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection

import cats.data.{EitherT, NonEmptyList}
import cats.effect.Concurrent
import org.http4s.client.Client


trait DevelopersHandler[F[_]] {

  def checkConnection(developers: Developers): EitherT[F, NonEmptyList[Error], Connection]

}

object DevelopersHandler {

  def checkConnection[F[_] : Concurrent : Client](github: GitHubService[F],
                                                  developers: Developers)
                                                 (implicit ghConnection: GitHubConnection,
                                                  twConnection: TwitterConnection): EitherT[F, NonEmptyList[Error], Connection] =
    EitherT.pure(NotConnected)

  def default[F[_] : Concurrent : Client](github: GitHubService[F])
                                         (implicit connection: GitHubConnection,
                                          twConnection: TwitterConnection): DevelopersHandler[F] =
    (developers: Developers) => DevelopersHandler.checkConnection(github, developers)

}
