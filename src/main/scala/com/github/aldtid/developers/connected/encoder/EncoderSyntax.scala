package com.github.aldtid.developers.connected.encoder


class EncoderSyntax[A](a: A) {

  implicit def encode[O](implicit encoder: Encoder[A, O]): O = encoder.encode(a)

}
