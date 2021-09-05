package com.github.aldtid.developers.connected.encoder


/**
 * Enhances the functionality of a generic type instance adding Encoder functions for that type.
 *
 * @param a instance to enhance
 * @tparam A generic type
 */
class EncoderSyntax[A](a: A) {

  implicit def encode[O](implicit encoder: Encoder[A, O]): O = encoder.encode(a)

}
