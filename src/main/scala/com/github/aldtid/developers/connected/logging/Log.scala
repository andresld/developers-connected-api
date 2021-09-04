package com.github.aldtid.developers.connected.logging

import cats.Semigroup


/**
 * Representation of a message to be logged.
 *
 * This structure is based on the combination of different structures, for whom a specific representation must be
 * defined (Loggable instances). In that case, the combination of a Log and a structure with a Loggable results in a
 * new Log.
 *
 * Because of this, we can think on Log structures as the representation of a previous chain of instances formatted for
 * a certain logging type L.
 *
 * @tparam L type to format the logs
 */
trait Log[L] {

  /**
   * Current representation of this Log formatted as an L instance.
   */
  val value: L

  /**
   * Combines current Log with an A instance, resulting in a new Log.
   *
   * @param a instance to incorporate to the log
   * @param loggable representation for that A instance
   * @tparam A generic type to log
   * @return a new Log with the representation of passed A instance as part of it
   */
  def  |+|[A](a: A)(implicit loggable: Loggable[A, L]): Log[L]

  /**
   * Formats the Log value into a String representation.
   *
   * @return the string representation of Log value
   */
  def formatted: String

}

object Log {

  /**
   * Creates a Log instance defining a base value and the formatting function for the new Log.
   *
   * The combination of this Log and a loggable instance results in a new Log whose base value combines current value
   * and argument formatted value.
   *
   * @param _value base Log value
   * @param format formatting function to represent the Log as a String
   * @tparam L type to format the logs
   * @return a new Log instance which format as a L type
   */
  def createLog[L : Semigroup](_value: L, format: L => String): Log[L] = new Log[L] {

    val value: L = _value

    def |+|[A](a: A)(implicit loggable: Loggable[A, L]): Log[L] =
      createLog(Semigroup[L].combine(value, loggable.format(a)), format)

    def formatted: String = format(value)

  }

  /**
   * Enables the implicit casting of a Log instance as a String.
   *
   * This is useful to simplify the requirement to call the 'formatted' function when passing the value to a logger.
   *
   * @param log instance to cast
   * @tparam L type to format the logs
   * @return the String representation of the Log
   */
  implicit def formatted[L](log: Log[L]): String = log.formatted

}
