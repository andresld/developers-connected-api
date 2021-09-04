package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.model.ModelSyntax


object implicits {

  implicit object all extends CastingOps with ModelSyntax
  implicit object model extends ModelSyntax

}
