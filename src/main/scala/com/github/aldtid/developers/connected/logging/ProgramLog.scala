package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.configuration.Server
import com.github.aldtid.developers.connected.logging.model._
import com.github.aldtid.developers.connected.model.responses.{Connection, Errors}
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.response._
import com.github.aldtid.developers.connected.service.github.error.{Error => GError}
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.service.twitter.response._
import com.github.aldtid.developers.connected.service.twitter.error.{Error => TError}

import org.http4s.{Request, Response}
import pureconfig.error.ConfigReaderFailures


/**
 * Representation of the generic logging system for current program environment.
 *
 * Any implementation for this representation should add a Log instance, to combine the different logs, and multiple
 * Loggable instances, one per type to log, which will represent the later logging representation for that structure
 * according to L type format.
 *
 * @tparam L type to format the logs
 */
trait ProgramLog[L] {

  // Logging instance
  implicit val log: Log[L]

  // Supported http4s types
  implicit def requestLoggable[F[_]]: Loggable[Request[F], L]
  implicit def responseLoggable[F[_]]: Loggable[Response[F], L]

  // Supported pureconfig types
  implicit val configReaderFailuresLoggable: Loggable[ConfigReaderFailures, L]

  // Supported common model types
  implicit val messageLoggable: Loggable[Message, L]
  implicit val tagLoggable: Loggable[Tag, L]
  implicit val usernameLoggable: Loggable[Username, L]
  implicit val identifierLoggable: Loggable[Identifier, L]
  implicit val latencyLoggable: Loggable[Latency, L]
  implicit val threadPoolLoggable: Loggable[ThreadPool, L]

  // Supported configuration model types
  implicit val configurationServerLoggable: Loggable[Server, L]

  // Supported API model types
  implicit val connectionLoggable: Loggable[Connection, L]
  implicit val errorsLoggable: Loggable[Errors, L]

  // Supported Twitter model types
  implicit val twitterUserDataLoggable: Loggable[UserData, L]
  implicit val twitterFollowersLoggable: Loggable[Followers, L]
  implicit val twitterErrorLoggable: Loggable[TError, L]
  implicit val twitterConnectionLoggable: Loggable[TwitterConnection, L]

  // Supported GitHub model types
  implicit val githubOrganizationsLoggable: Loggable[List[Organization], L]
  implicit val githubErrorLoggable: Loggable[GError, L]
  implicit val githubConnectionLoggable: Loggable[GitHubConnection, L]

}
