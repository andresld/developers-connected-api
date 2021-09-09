package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.service.github.error.{Error => GError}
import com.github.aldtid.developers.connected.service.twitter.error.{Error => TError}

import cats.data.NonEmptyList


object model {

  object responses {

    sealed trait Connection {
      val connection: Boolean
    }

    final case class Connected(organizations: NonEmptyList[String]) extends Connection {
      val connection: Boolean = true
    }

    object Connected {
      def of(head: String, tail: String*): Connected = Connected(NonEmptyList.of(head, tail: _*))
    }

    final case object NotConnected extends Connection {
      val connection: Boolean = false
    }

    sealed trait Error
    final case class InvalidGitHubUser(username: String) extends Error
    final case class InvalidTwitterUser(username: String) extends Error
    final case class InternalGitHubError(username: String, error: GError) extends Error
    final case class InternalTwitterError(username: String, error: TError) extends Error
    final case object InterruptedExecution extends Error
    final case object MissingResource extends Error

    final case class Errors(errors: NonEmptyList[Error])

    object Errors {
      def of(head: Error, tail: Error*): Errors = Errors(NonEmptyList.of(head, tail: _*))
    }

  }

  final case class Developers(first: String, second: String, others: String*)

}
