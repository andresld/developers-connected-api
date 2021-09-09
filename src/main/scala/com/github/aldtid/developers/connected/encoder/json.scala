package com.github.aldtid.developers.connected.encoder

import com.github.aldtid.developers.connected.model.responses._

import io.circe.{Json, Encoder => CEncoder}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoder


/**
 * Json representation for body encodings.
 */
object json {

  // ----- ENCODING INSTANCES -----
  val jsonConnectionEncoder: Encoder[Connection, Json] = connection => {

    val base: Json = connection.asJson

    connection match {
      case c: Connected => base deepMerge c.asJson
      case NotConnected => base
    }

  }

  val jsonErrorsEncoder: Encoder[Errors, Json] = _.asJson
  // ----------

  // ----- CIRCE INSTANCES -----
  implicit val jsonConnectionCirceEncoder: CEncoder[Connection] =
    connection => Json.obj("connected" -> connection.connection.asJson)

  implicit val jsonErrorCirceEncoder: CEncoder[Error] = {
    case InvalidGitHubUser(username) => s"$username is not a valid user in github".asJson
    case InvalidTwitterUser(username) => s"$username is not a valid user in twitter".asJson
    case InternalGitHubError(username, _) => s"internal error with $username in github".asJson
    case InternalTwitterError(username, _) => s"internal error with $username in twitter".asJson
    case MissingResource => "missing resource".asJson
    case InterruptedExecution => "interrupted execution".asJson
  }
  // ----------

  implicit val jsonResponseEncoder: BodyEncoder[Json] = new BodyEncoder[Json] {

    implicit def entityEncoder[F[_]]: EntityEncoder[F, Json] = jsonEncoder

    implicit val connectionEncoder: Encoder[Connection, Json] = jsonConnectionEncoder
    implicit val errorsEncoder: Encoder[Errors, Json] = jsonErrorsEncoder

  }

}
