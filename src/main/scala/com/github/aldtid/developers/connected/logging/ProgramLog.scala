package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.model.{Latency, Message}
import org.http4s.{Request, Response}


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

  // Supported types to be logged
  implicit def requestLoggable[F[_]]: Loggable[Request[F], L]
  implicit def responseLoggable[F[_]]: Loggable[Response[F], L]
  implicit val messageLoggable: Loggable[Message, L]
  implicit val latencyLoggable: Loggable[Latency, L]

}
