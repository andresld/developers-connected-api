package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.implicits.model._
import com.github.aldtid.developers.connected.logging.model.Message


object messages {

  // ---- GENERAL ROUTES -----
  val incomingRequest: Message = "incoming request".asMessage
  val outgoingResponse: Message = "outgoing response".asMessage

  // ----- TWITTER SERVICE -----
  val twitterUserRequest: Message = "requesting user by username".asMessage
  val twitterUserResponse: Message = "response for requested username".asMessage
  val twitterUserSuccess: Message = "user retrieved".asMessage
  val twitterUserError: Message = "user could not be retrieved".asMessage

  val twitterFollowersRequest: Message = "requesting user followers".asMessage
  val twitterFollowersResponse: Message = "response for requested followers".asMessage
  val twitterFollowersSuccess: Message = "user followers retrieved".asMessage
  val twitterFollowersError: Message = "user followers could not be retrieved".asMessage

  // ----- GITHUB SERVICE -----
  val githubOrganizationsRequest: Message = "requesting user organizations".asMessage
  val githubOrganizationsResponse: Message = "response for requested organizations".asMessage
  val githubOrganizationsSuccess: Message = "organizations retrieved".asMessage
  val githubOrganizationsError: Message = "organizations could not be retrieved".asMessage

}
