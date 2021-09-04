package com.github.aldtid.developers.connected.logging

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class CastingOpsTests extends AnyFlatSpec with Matchers {

  object ops$$ extends CastingOps

  "loggableAsLog" should "transform an instance into a Log structure as expected" in {

    import ops$$._

    implicit val log: Log[String] = Log.createLog[String]("", "log: " + _)(_ + "" + _)
    implicit val loggable: Loggable[Int, String] = _.toString

    val value: Log[String] = 1

    value.value shouldBe "1"
    value.formatted shouldBe "log: 1"

  }

}
