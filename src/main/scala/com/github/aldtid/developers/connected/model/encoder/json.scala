package com.github.aldtid.developers.connected.model.encoder

import com.github.aldtid.developers.connected.model.responses._

import io.circe.{Encoder => CEncoder, Json}
import io.circe.generic.auto._
import io.circe.syntax._


object json {

  val jsonConnectionEncoder: Encoder[Connection, Json] = connection => {

    val base: Json = connection.asJson

    connection match {
      case Connected => base
      case c: NotConnected => base deepMerge c.asJson
    }

  }

  val jsonErrorsEncoder: Encoder[Errors, Json] = _.asJson

  implicit val jsonConnectionCirceEncoder: CEncoder[Connection] =
    connection => Json.obj("connected" -> connection.connection.asJson)

  implicit val jsonMissingResourceCirceEncoder: CEncoder[MissingResource.type] =
    _ => "missing resource".asJson

  implicit val jsonResponseEncoder: ResponseEncoder[Json] = new ResponseEncoder[Json] {

    implicit val connectionEncoder: Encoder[Connection, Json] = jsonConnectionEncoder
    implicit val errorsEncoder: Encoder[Errors, Json] = jsonErrorsEncoder

  }

}
