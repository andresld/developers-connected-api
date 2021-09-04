package com.github.aldtid.developers.connected.logging.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ModelSyntaxTests extends AnyFlatSpec with Matchers {

  object ext extends ModelSyntax

  "stringExtensions" should "correctly pimp a string value with logging conversion functions" in {

    import ext._

    "test".asMessage shouldBe Message("test")

  }

  "longExtensions" should "correctly pimp a long value with logging conversion functions" in {

    import ext._

    1.asLatency shouldBe Latency(1)

  }

}
