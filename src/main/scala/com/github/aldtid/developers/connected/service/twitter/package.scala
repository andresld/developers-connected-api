package com.github.aldtid.developers.connected.service

import io.circe.{Json, Error => CError}
import org.http4s.Uri


package object twitter {

  object connection {

    final case class TwitterConnection(baseUri: Uri, token: String)

  }

  object response {

    final case class User(id: String, name: String, username: String)

    final case class UserData(data: Option[User], errors: Option[List[Json]])

    final case class Meta(resultCount: Long)

    final case class Following(data: Option[List[User]], meta: Option[Meta], errors: Option[List[Json]])

  }

  object error {

    sealed trait Error
    final case class BadRequest(body: String) extends Error
    final case class Unauthorized(body: String) extends Error
    final case class UnexpectedResponse(status: Int, body: String, error: Option[CError]) extends Error

  }

}
