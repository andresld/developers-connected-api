package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.implicits.model._
import com.github.aldtid.developers.connected.logging.model.Tag


object tags {

  val launcherTag: Tag = "LAUNCHER".asTag
  val routerTag: Tag = "ROUTER".asTag
  val githubTag: Tag = "GITHUB".asTag
  val twitterTag: Tag = "TWITTER".asTag

}
