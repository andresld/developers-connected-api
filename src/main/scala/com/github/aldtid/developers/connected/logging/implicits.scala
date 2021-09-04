package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.model.ModelExtensions

object implicits {

  implicit object all extends LogOps with ModelExtensions
  implicit object model extends ModelExtensions

}
