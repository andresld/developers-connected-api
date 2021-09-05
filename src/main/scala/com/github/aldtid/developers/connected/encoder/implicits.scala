package com.github.aldtid.developers.connected.encoder


object implicits {

  implicit def encoderSyntax[A](a: A): EncoderSyntax[A] = new EncoderSyntax(a)

}
