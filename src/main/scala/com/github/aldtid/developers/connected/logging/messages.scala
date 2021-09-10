package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.implicits.model._
import com.github.aldtid.developers.connected.logging.model.Message


object messages {

  // ---- LAUNCHER -----
  val loadingConfiguration: Message = "loading configuration".asMessage
  val configurationLoaded: Message = "configuration loaded".asMessage
  val configurationErrors: Message = "configuration had errors".asMessage
  val clientThreadPool: Message = "creating client thread pool".asMessage
  val serverThreadPool: Message = "creating server thread pool".asMessage
  val creatingClient: Message = "creating http client".asMessage
  val githubConnection: Message = "GitHub connection details".asMessage
  val twitterConnection: Message = "Twitter connection details".asMessage
  val startingServer: Message = "starting the server".asMessage

  // ---- GENERAL ROUTES -----
  val incomingRequest: Message = "incoming request".asMessage
  val outgoingResponse: Message = "outgoing response".asMessage

  // ----- DEVELOPERS HANDLER -----
  val connectionResult: Message = "connection result".asMessage
  val connectionErrors: Message = "errors found when checking for connection".asMessage
  val retrieveOrgsCache: Message = "retrieving value from organizations cache".asMessage
  val evalOrgsCache: Message = "evaluating value to update organizations cache".asMessage
  val retrieveUsersCache: Message = "retrieving value from users cache".asMessage
  val evalUsersCache: Message = "evaluating value to update users cache".asMessage

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
