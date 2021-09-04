package com.github.aldtid.developers.connected.logging

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class LogTests extends AnyFlatSpec with Matchers {

  "createLog" should "create the expected Log instance with passed arguments" in {

    val log: Log[String] = Log.createLog[String]("", "log: " + _)(_ + "" + _)

    log.value shouldBe ""
    log.formatted shouldBe "log: "

    implicit val loggable: Loggable[Int, String] = _.toString

    val value: Log[String] = log |+| 1

    value.value shouldBe "1"
    value.formatted shouldBe "log: 1"

  }

  "formatted" should "cast a Log instance to a String value using the formatted function" in {

    val value: String = Log.createLog[String]("", "log: " + _)(_ + "" + _)

    value shouldBe "log: "

  }

}
