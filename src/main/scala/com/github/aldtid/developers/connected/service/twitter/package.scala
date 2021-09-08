package com.github.aldtid.developers.connected.service

import io.circe.{Error => CError}
import org.http4s.Uri


package object twitter {

  object connection {

    final case class TwitterConnection(baseUri: Uri, token: String)

  }

  object response {

    final case class User(id: String, name: String, username: String)

    final case class UserData(data: User)

    final case class Meta(resultCount: Long)

    final case class Followers(data: Option[List[User]], meta: Meta)

  }

  object error {

    sealed trait Error
    final case class NotFound(body: String) extends Error
    final case class UnexpectedResponse(status: Int, body: String, error: Option[CError]) extends Error

  }

}
