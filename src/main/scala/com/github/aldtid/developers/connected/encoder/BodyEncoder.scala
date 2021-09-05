package com.github.aldtid.developers.connected.encoder

import com.github.aldtid.developers.connected.model.responses.{Connection, Errors}
import org.http4s.EntityEncoder


trait BodyEncoder[O] {

  implicit def entityEncoder[F[_]]: EntityEncoder[F, O]

  implicit val connectionEncoder: Encoder[Connection, O]
  implicit val errorsEncoder: Encoder[Errors, O]

}
