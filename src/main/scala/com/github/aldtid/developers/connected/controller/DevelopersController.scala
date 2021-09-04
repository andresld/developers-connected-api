package com.github.aldtid.developers.connected.controller

import com.github.aldtid.developers.connected.model.Developers
import com.github.aldtid.developers.connected.model.responses.{Connected, Connection, Error}

import cats.Applicative
import cats.data.{EitherT, NonEmptyList}


trait DevelopersController[F[_]] {

  def checkConnection(developers: Developers): EitherT[F, NonEmptyList[Error], Connection]

}

object DevelopersController {

  def checkConnection[F[_] : Applicative](developers: Developers): EitherT[F, NonEmptyList[Error], Connection] =
    EitherT.pure(Connected)

  def default[F[_] : Applicative]: DevelopersController[F] =
    (developers: Developers) => DevelopersController.checkConnection(developers)

}
