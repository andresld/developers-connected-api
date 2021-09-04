package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.Log.createLog
import com.github.aldtid.developers.connected.logging.model.{Latency, Message}

import io.circe.{Json, Printer}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{Header, Request, Response}


/**
 * Json representation for program logs.
 */
object json {

  // ----- BASE LOG INSTANCE -----
  val jsonLog: Log[Json] = createLog[Json](Json.obj(), _.printWith(Printer.noSpaces))(_ deepMerge _)
  // ----------

  // ----- LOGGABLE INSTANCES -----
  def jsonRequestLoggable[F[_]]: Loggable[Request[F], Json] = request =>
    Json.obj(
      ("request", Json.obj(
        ("method", Json.fromString(request.method.name)),
        ("uri", Json.fromString(request.uri.renderString)),
        ("version", Json.fromString(request.httpVersion.renderString)),
        ("headers", Json.fromValues(request.headers.headers.map(toJson)))
      ))
    )

  def jsonResponseLoggable[F[_]]: Loggable[Response[F], Json] = response =>
    Json.obj(
      ("response", Json.obj(
        ("status", Json.fromInt(response.status.code)),
        ("version", Json.fromString(response.httpVersion.renderString)),
        ("headers", Json.fromValues(response.headers.headers.map(toJson)))
      ))
    )

  val jsonMessageLoggable: Loggable[Message, Json] = _.asJson

  val jsonLatencyLoggable: Loggable[Latency, Json] = _.asJson
  // ----------

  // ----- UTILITY FUNCTIONS -----
  def toJson(raw: Header.Raw): Json = Json.obj((raw.name.toString, Json.fromString(raw.value)))
  // ----------

  implicit val jsonProgramLog: ProgramLog[Json] = new ProgramLog[Json] {

    implicit val log: Log[Json] = jsonLog

    implicit def requestLoggable[F[_]]: Loggable[Request[F], Json] = jsonRequestLoggable
    implicit def responseLoggable[F[_]]: Loggable[Response[F], Json] = jsonResponseLoggable
    implicit val messageLoggable: Loggable[Message, Json] = jsonMessageLoggable
    implicit val latencyLoggable: Loggable[Latency, Json] = jsonLatencyLoggable

  }

}
