package com.github.aldtid.developers.connected.logging


/**
 * Utility functions to perform implicit castings for generic types to logging instances.
 *
 * These castings have the function to simply the initial conversion from any type to a type that can be logged
 * (typically a String) or to a proper Log instance.
 */
trait CastingOps {

  /**
   * Casts an instance of a generic type into a log representation using an existing loggable instance for that type.
   *
   * @param a instance to log
   * @param log current log representation
   * @param loggable loggable representation for a type A to be formatted as L
   * @tparam A type to log
   * @tparam L type to format the logs
   * @return the resulting combination of a Log and passed A instance
   */
  implicit def asLog[A, L](a: A)(implicit log: Log[L], loggable: Loggable[A, L]): Log[L] = log |+| a

}
