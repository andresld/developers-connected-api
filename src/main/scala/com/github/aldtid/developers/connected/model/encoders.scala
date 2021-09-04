package com.github.aldtid.developers.connected.model

import com.github.aldtid.developers.connected.model.responses._

import io.circe.{Encoder, Errors}
import io.circe.generic.auto._
import io.circe.syntax._


object encoders {

  implicit val errorsEncoder: Encoder[Errors] = _.asJson
  implicit val missingResourceEncoder: Encoder[MissingResource.type] = _ => "missing resource".asJson

}
