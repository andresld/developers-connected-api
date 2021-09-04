package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import com.github.aldtid.developers.connected.logging.model.{Latency, Message}

import cats.Id
import io.circe.Json
import org.http4s.Header.Raw
import org.http4s.{Headers, Request, Response}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIString


class JsonTests extends AnyFlatSpec with Matchers {

  "jsonProgrammingLog" should "define a Log[Json] instance with the expected behaviour" in {

    import jsonProgramLog.{latencyLoggable, messageLoggable}

    val log: Log[Json] = jsonProgramLog.log

    log.value shouldBe Json.obj()
    log.formatted shouldBe "{}"

    val value1: Log[Json] = log |+| Latency(1)

    value1.value shouldBe Json.obj("latency" -> Json.fromLong(1))
    value1.formatted shouldBe """{"latency":1}"""

    val value2: Log[Json] = value1 |+| Message("test")

    value2.value shouldBe Json.obj("latency" -> Json.fromLong(1), "message"-> Json.fromString("test"))
    value2.formatted shouldBe """{"message":"test","latency":1}"""

  }

  it should "define Loggable instances with the expected formats" in {

    val request: Request[Id] = Request(headers = Headers(Raw(CIString("header"), "value")))
    val response: Response[Id] = Response(headers = Headers(Raw(CIString("header"), "value")))

    jsonProgramLog.requestLoggable.format(request) shouldBe
      Json.obj(
        "request" -> Json.obj(
          "method" -> Json.fromString("GET"),
          "uri" -> Json.fromString("/"),
          "version" -> Json.fromString("HTTP/1.1"),
          "headers" -> Json.obj(
            "header" -> Json.fromString("value")
          )
        )
      )

    jsonProgramLog.responseLoggable.format(response) shouldBe
      Json.obj(
        "response" -> Json.obj(
          "status" -> Json.fromInt(200),
          "version" -> Json.fromString("HTTP/1.1"),
          "headers" -> Json.obj(
            "header" -> Json.fromString("value")
          )
        )
      )

    jsonProgramLog.latencyLoggable.format(Latency(1)) shouldBe Json.obj("latency" -> Json.fromInt(1))
    jsonProgramLog.messageLoggable.format(Message("test")) shouldBe Json.obj("message" -> Json.fromString("test"))

  }

}
