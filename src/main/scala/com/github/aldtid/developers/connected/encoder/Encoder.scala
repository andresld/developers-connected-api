package com.github.aldtid.developers.connected.encoder


trait Encoder[A, O] {

  def encode(a: A): O

}
