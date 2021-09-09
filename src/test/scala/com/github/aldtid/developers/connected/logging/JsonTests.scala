package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.logging.json.{errorEncoder, jsonProgramLog}
import com.github.aldtid.developers.connected.logging.model._
import com.github.aldtid.developers.connected.service.github.{error => gerror}
import com.github.aldtid.developers.connected.service.github.response.Organization
import com.github.aldtid.developers.connected.service.twitter.{error => terror}
import com.github.aldtid.developers.connected.service.twitter.response.{Followers, Meta, User, UserData}
import cats.Id
import cats.data.NonEmptyList
import com.github.aldtid.developers.connected.configuration.Server
import com.github.aldtid.developers.connected.model.responses._
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import io.circe.Json
import org.http4s.Header.Raw
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Headers, Request, Response}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIString
import pureconfig.error.{CannotConvert, ConfigReaderFailures, ConvertFailure}


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

    jsonProgramLog.configReaderFailuresLoggable
      .format(ConfigReaderFailures(ConvertFailure(CannotConvert("value", "type", "reason"), None, "key"))) shouldBe
        Json.obj("failures" -> Json.fromString("at 'key':\n  - Cannot convert 'value' to type: reason."))

    jsonProgramLog.messageLoggable.format(Message("test")) shouldBe Json.obj("message" -> Json.fromString("test"))
    jsonProgramLog.tagLoggable.format(Tag("test")) shouldBe Json.obj("tag" -> Json.fromString("test"))
    jsonProgramLog.usernameLoggable.format(Username("test")) shouldBe Json.obj("username" -> Json.fromString("test"))
    jsonProgramLog.identifierLoggable.format(Identifier("test")) shouldBe Json.obj("id" -> Json.fromString("test"))
    jsonProgramLog.latencyLoggable.format(Latency(1)) shouldBe Json.obj("latency" -> Json.fromInt(1))
    jsonProgramLog.threadPoolLoggable.format(ThreadPool(1)) shouldBe Json.obj("threadPool" -> Json.fromInt(1))

    jsonProgramLog.configurationServerLoggable.format(Server("host", 80, "/root")) shouldBe
      Json.obj(
        "server" -> Json.obj(
          "host" -> Json.fromString("host"),
          "port" -> Json.fromInt(80),
          "basePath" -> Json.fromString("/root")
        )
      )

    jsonProgramLog.connectionLoggable.format(NotConnected) shouldBe Json.obj("connected" -> Json.fromBoolean(false))
    jsonProgramLog.connectionLoggable.format(Connected(NonEmptyList.one("org"))) shouldBe Json.obj("connected" -> Json.fromBoolean(true))

    jsonProgramLog.errorsLoggable.format(Errors(NonEmptyList.one(MissingResource))) shouldBe
      Json.obj(
        "errors" ->
          Json.arr(
            Json.obj(
              "missing" -> Json.fromString("resource")
            )
          )
      )

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

    jsonProgramLog.twitterConnectionLoggable.format(TwitterConnection(uri"http://localhost:80", "token")) shouldBe
      Json.obj(
        "twitter" -> Json.obj(
          "uri" -> Json.fromString("http://localhost:80")
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

    jsonProgramLog.githubConnectionLoggable.format(GitHubConnection(uri"http://localhost:80", "username", "token")) shouldBe
      Json.obj(
        "github" -> Json.obj(
          "uri" -> Json.fromString("http://localhost:80"),
          "username" -> Json.fromString("username")
        )
      )

  }

  "errorEncoder" should "convert the different error instances as expected" in {

    errorEncoder.apply(InvalidGitHubUser("dev1")) shouldBe
      Json.obj(
        "invalid" -> Json.obj(
          "github" -> Json.obj(
            "username" -> Json.fromString("dev1")
          )
        )
      )

    errorEncoder.apply(InvalidTwitterUser("dev1")) shouldBe
      Json.obj(
        "invalid" -> Json.obj(
          "twitter" -> Json.obj(
            "username" -> Json.fromString("dev1")
          )
        )
      )

    errorEncoder.apply(InternalGitHubError("dev1", gerror.NotFound("body"))) shouldBe
      Json.obj(
        "internal" -> Json.obj(
          "github" -> Json.obj(
            "username" -> Json.fromString("dev1"),
            "error" -> Json.obj(
              "notFound" -> Json.obj(
                "body" -> Json.fromString("body")
              )
            )
          )
        )
      )

    errorEncoder.apply(InternalTwitterError("dev1", terror.NotFound("body"))) shouldBe
      Json.obj(
        "internal" -> Json.obj(
          "twitter" -> Json.obj(
            "username" -> Json.fromString("dev1"),
            "error" -> Json.obj(
              "notFound" -> Json.obj(
                "body" -> Json.fromString("body")
              )
            )
          )
        )
      )

    errorEncoder.apply(MissingResource) shouldBe Json.obj("missing" -> Json.fromString("resource"))
    errorEncoder.apply(InterruptedExecution) shouldBe Json.obj("interrupted" -> Json.fromString("execution"))

  }

}
