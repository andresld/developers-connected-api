package com.github.aldtid.developers.connected.logging


/**
 * Represents the formatting for an instance of type A as a L type. Its use is intended for types representations in
 * logging formats.
 *
 * @tparam A type to be formatted
 * @tparam L type to format the logs
 */
trait Loggable[-A, L] {

  /**
   * Transforms an instance of type A as an instance of type L.
   *
   * @param a instance to transform
   * @return the respective L instance for passed argument
   */
  def format(a: A): L

}
