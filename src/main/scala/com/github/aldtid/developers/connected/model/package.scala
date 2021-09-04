package com.github.aldtid.developers.connected

import cats.data.NonEmptyList

package object model {

  object responses {

    sealed trait Connection {
      val connection: Boolean
    }

    final case object Connected extends Connection {
      val connection: Boolean = true
    }

    final case class NotConnected(organizations: List[String]) extends Connection {
      val connection: Boolean = false
    }

    sealed trait Error
    final case class InvalidGitHubUser(developer: String) extends Error
    final case class InvalidTwitterUser(developer: String) extends Error
    final case object MissingResource extends Error

    final case class Errors(errors: NonEmptyList[Error])

    object Errors {

      def of(head: Error, tail: Error*): Errors = Errors(NonEmptyList.of(head, tail: _*))

    }

  }

  final case class Developers(first: String, second: String, others: String*)

}
