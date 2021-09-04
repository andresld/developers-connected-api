package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.implicits.model._
import com.github.aldtid.developers.connected.logging.model.Message


object messages {

  val incomingRequest: Message = "incoming request".asMessage
  val outgoingResponse: Message = "outgoing response".asMessage

}
