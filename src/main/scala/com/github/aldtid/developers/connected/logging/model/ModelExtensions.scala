package com.github.aldtid.developers.connected.logging.model

import com.github.aldtid.developers.connected.logging.model.extensions.{LongExtensions, StringExtensions}


/**
 * Contains the extension methods to be injected for base types.
 */
trait ModelExtensions {

  implicit def stringExtensions(value: String): StringExtensions = new StringExtensions(value)
  implicit def longExtensions(value: Long): LongExtensions = new LongExtensions(value)

}
