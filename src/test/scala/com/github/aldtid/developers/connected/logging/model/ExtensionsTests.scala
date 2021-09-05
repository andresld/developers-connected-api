package com.github.aldtid.developers.connected.logging.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ExtensionsTests extends AnyFlatSpec with Matchers {

  "StringExtensions" should "provide the expected values for the conversion functions" in {

    val ext: syntax.StringSyntax = new syntax.StringSyntax("test")

    ext.asMessage shouldBe Message("test")

  }

  "LongExtensions" should "provide the expected values for the conversion functions" in {

    val ext: syntax.LongSyntax = new syntax.LongSyntax(1)

    ext.asLatency shouldBe Latency(1)

  }

}
