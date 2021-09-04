package com.github.aldtid.developers.connected.logging.model

import com.github.aldtid.developers.connected.logging.model.syntax.{LongSyntax, StringSyntax}


/**
 * Contains the extension methods to be injected for base types.
 */
trait ModelSyntax {

  implicit def stringSyntax(value: String): StringSyntax = new StringSyntax(value)
  implicit def longSyntax(value: Long): LongSyntax = new LongSyntax(value)

}
