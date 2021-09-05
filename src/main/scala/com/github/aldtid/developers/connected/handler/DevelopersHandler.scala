package com.github.aldtid.developers.connected.handler

import com.github.aldtid.developers.connected.model.Developers
import com.github.aldtid.developers.connected.model.responses.{Connected, Connection, Error, NotConnected}
import cats.Applicative
import cats.data.{EitherT, NonEmptyList}


trait DevelopersHandler[F[_]] {

  def checkConnection(developers: Developers): EitherT[F, NonEmptyList[Error], Connection]

}

object DevelopersHandler {

  def checkConnection[F[_] : Applicative](developers: Developers): EitherT[F, NonEmptyList[Error], Connection] =
    EitherT.pure(NotConnected)

  def default[F[_] : Applicative]: DevelopersHandler[F] =
    (developers: Developers) => DevelopersHandler.checkConnection(developers)

}
