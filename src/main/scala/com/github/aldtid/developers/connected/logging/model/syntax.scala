package com.github.aldtid.developers.connected.logging.model


/**
 * Contains the types for base types extension methods to simplify the instantiation of logging types.
 */
object syntax {

  class StringSyntax(value: String) {
    def asMessage: Message = Message(value)
    def asTag: Tag = Tag(value)
    def asUsername: Username = Username(value)
    def asIdentifier: Identifier = Identifier(value)
  }

  class LongSyntax(value: Long) {
    def asLatency: Latency = Latency(value)
  }

}
