package com.github.aldtid.developers.connected.model.encoder


trait Encoder[A, O] {

  def encode(a: A): O

}
