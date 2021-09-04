package com.github.aldtid.developers.connected.logging.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ExtensionsTests extends AnyFlatSpec with Matchers {

  "StringExtensions" should "provide the expected values for the conversion functions" in {

    val ext: extensions.StringExtensions = new extensions.StringExtensions("test")

    ext.asMessage shouldBe Message("test")

  }

  "LongExtensions" should "provide the expected values for the conversion functions" in {

    val ext: extensions.LongExtensions = new extensions.LongExtensions(1)

    ext.asLatency shouldBe Latency(1)

  }

}
