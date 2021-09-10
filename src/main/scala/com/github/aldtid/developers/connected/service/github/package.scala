package com.github.aldtid.developers.connected.service

import io.circe.{Error => CError}
import org.http4s.Uri


package object github {

  object connection {

    final case class GitHubConnection(baseUri: Uri, username: String, token: String)

  }

  object response {

    // Only matches required fields to identify an organization, as the rest of them will not be used
    final case class Organization(login: String, id: Long)

  }

  object error {

    sealed trait Error
    final case class NotFound(body: String) extends Error
    final case class Unauthorized(body: String) extends Error
    final case class UnexpectedResponse(status: Int, body: String, error: Option[CError]) extends Error

  }

}
