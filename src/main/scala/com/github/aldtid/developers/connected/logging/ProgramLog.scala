package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.model.{Latency, Message}
import org.http4s.{Request, Response}

trait ProgramLog[L] {

  // Logging instance
  implicit val log: Log[L]

  // Supported types to be logged
  implicit def requestLoggable[F[_]]: Loggable[Request[F], L]
  implicit def responseLoggable[F[_]]: Loggable[Response[F], L]
  implicit val messageLoggable: Loggable[Message, L]
  implicit val latencyLoggable: Loggable[Latency, L]

}
