package com.github.aldtid.developers.connected.encoder

import com.github.aldtid.developers.connected.model.responses.{Connection, Errors}
import org.http4s.EntityEncoder


/**
 * Represents the encoding for the possible API responses. It also defines an EntityEncoder, which is used to transform
 * the output type to be transformed into a response body.
 *
 * @tparam O type to be sent in response bodies
 */
trait BodyEncoder[O] {

  // Entity encoding instance
  implicit def entityEncoder[F[_]]: EntityEncoder[F, O]

  // Supported types to be encoded
  implicit val connectionEncoder: Encoder[Connection, O]
  implicit val errorsEncoder: Encoder[Errors, O]

}
