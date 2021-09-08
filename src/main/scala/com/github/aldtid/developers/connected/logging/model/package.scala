package com.github.aldtid.developers.connected.logging


/**
 * Contains the dedicated logging types, which serve no other purpose than logs.
 */
package object model {

  final case class Message(message: String)
  final case class Tag(tag: String)
  final case class Username(username: String)
  final case class Identifier(id: String)

  final case class Latency(latency: Long)

}
