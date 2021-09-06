package com.github.aldtid.developers.connected.service

import io.circe.{Error => CError}
import org.http4s.Uri


package object github {

  object connection {

    final case class GitHubConnection(baseUri: Uri, username: String, token: String)

  }

  object response {

    final case class Organization(login: String, id: Long)

  }

  object error {

    sealed trait Error
    final case class DefaultError(message: String, documentationUrl: String) extends Error
    final case class UnexpectedResponse(status: Int, body: String, error: CError) extends Error

  }

}
