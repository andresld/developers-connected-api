package com.github.aldtid.developers.connected.model.encoder

import com.github.aldtid.developers.connected.model.responses.{Connection, Errors}


trait ResponseEncoder[O] {

  implicit val connectionEncoder: Encoder[Connection, O]
  implicit val errorsEncoder: Encoder[Errors, O]

}
