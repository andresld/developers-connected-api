package com.github.aldtid.developers.connected.logging.model


/**
 * Contains the types for base types extension methods to simplify the instantiation of logging types.
 */
object extensions {

  class StringExtensions(value: String) {
    def asMessage: Message = Message(value)
  }

  class LongExtensions(value: Long) {
    def asLatency: Latency = Latency(value)
  }

}
