package com.github.aldtid.developers.connected.encoder


/**
 * Represents an encoding for an instance of type A as a O type.
 *
 * @tparam A type to be encoded
 * @tparam O target encoding type
 */
trait Encoder[A, O] {

  /**
   * Transforms an instance of type A as an instance of type O.
   *
   * @param a instance to transform
   * @return the respective O instance for passed argument
   */
  def encode(a: A): O

}
