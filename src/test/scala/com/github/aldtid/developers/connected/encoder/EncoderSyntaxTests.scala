package com.github.aldtid.developers.connected.encoder

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class EncoderSyntaxTests extends AnyFlatSpec with Matchers {

  "encode" should "apply the encoding for a type with an implicit encoder" in {

    val syntax: EncoderSyntax[Int] = new EncoderSyntax[Int](1)
    implicit val encoder: Encoder[Int, String] = int => s"value is $int"

    syntax.encode shouldBe "value is 1"

  }

}
