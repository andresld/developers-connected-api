package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import com.github.aldtid.developers.connected.logging.model._
import com.github.aldtid.developers.connected.service.github.{error => gerror}
import com.github.aldtid.developers.connected.service.github.response.Organization
import com.github.aldtid.developers.connected.service.twitter.{error => terror}
import com.github.aldtid.developers.connected.service.twitter.response.{Followers, Meta, User, UserData}

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
    value2.formatted shouldBe """{"latency":1,"message":"test"}"""

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

    jsonProgramLog.messageLoggable.format(Message("test")) shouldBe Json.obj("message" -> Json.fromString("test"))
    jsonProgramLog.tagLoggable.format(Tag("test")) shouldBe Json.obj("tag" -> Json.fromString("test"))
    jsonProgramLog.usernameLoggable.format(Username("test")) shouldBe Json.obj("username" -> Json.fromString("test"))
    jsonProgramLog.identifierLoggable.format(Identifier("test")) shouldBe Json.obj("id" -> Json.fromString("test"))
    jsonProgramLog.latencyLoggable.format(Latency(1)) shouldBe Json.obj("latency" -> Json.fromInt(1))

    jsonProgramLog.twitterUserDataLoggable.format(UserData(User("123", "name", "username"))) shouldBe
      Json.obj(
        "user" -> Json.obj(
          "id" -> Json.fromString("123"),
          "name" -> Json.fromString("name"),
          "username" -> Json.fromString("username")
        )
      )

    jsonProgramLog.twitterFollowersLoggable.format(Followers(Some(List(User("123", "name", "username"))), Meta(1))) shouldBe
      Json.obj(
        "followers" -> Json.arr(
          Json.obj(
            "id" -> Json.fromString("123"),
            "name" -> Json.fromString("name"),
            "username" -> Json.fromString("username")
          )
        ),
        "meta" -> Json.obj(
          "resultCount" -> Json.fromInt(1)
        )
      )

    jsonProgramLog.twitterFollowersLoggable.format(Followers(None, Meta(1))) shouldBe
      Json.obj(
        "followers" -> Json.Null,
        "meta" -> Json.obj(
          "resultCount" -> Json.fromInt(1)
        )
      )

    jsonProgramLog.twitterErrorLoggable.format(terror.NotFound("body")) shouldBe
      Json.obj(
        "error" -> Json.obj(
          "notFound" -> Json.obj(
            "body" -> Json.fromString("body")
          )
        )
      )

    jsonProgramLog.twitterErrorLoggable.format(terror.UnexpectedResponse(400, "body", None)) shouldBe
      Json.obj(
        "error" -> Json.obj(
          "unexpected" -> Json.obj(
            "status" -> Json.fromInt(400),
            "body" -> Json.fromString("body"),
            "error" -> Json.Null,
          )
        )
      )

    jsonProgramLog.githubOrganizationsLoggable.format(List(Organization("login", 123))) shouldBe
      Json.obj(
        "organizations" -> Json.arr(
          Json.obj(
            "login" -> Json.fromString("login"),
            "id" -> Json.fromInt(123)
          )
        )
      )

    jsonProgramLog.githubErrorLoggable.format(gerror.NotFound("body")) shouldBe
      Json.obj(
        "error" -> Json.obj(
          "notFound" -> Json.obj(
            "body" -> Json.fromString("body")
          )
        )
      )

    jsonProgramLog.githubErrorLoggable.format(gerror.UnexpectedResponse(400, "body", None)) shouldBe
      Json.obj(
        "error" -> Json.obj(
          "unexpected" -> Json.obj(
            "status" -> Json.fromInt(400),
            "body" -> Json.fromString("body"),
            "error" -> Json.Null,
          )
        )
      )

  }

}
