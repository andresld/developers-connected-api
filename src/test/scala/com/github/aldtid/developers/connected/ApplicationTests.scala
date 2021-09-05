package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.application._
import com.github.aldtid.developers.connected.encoder.json.jsonResponseEncoder
import com.github.aldtid.developers.connected.handler.DevelopersHandler
import com.github.aldtid.developers.connected.model.responses.{Connection, Error, InvalidGitHubUser, NotConnected}
import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import cats.Id
import cats.data.{EitherT, NonEmptyList}
import io.circe.Json
import org.http4s.{Headers, MediaType, Request, Response, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{`Content-Length`, `Content-Type`}
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ApplicationTests extends AnyFlatSpec with Matchers {

  "developers" should "return the expected Ok status if no errors happened" in {

    implicit val dsl: Http4sDsl[Id] = new Http4sDsl[Id] {}

    val handler: DevelopersHandler[Id] = developers =>
      if (developers.first == "dev1" && developers.second == "dev2") EitherT.rightT[Id, NonEmptyList[Error]](NotConnected)
      else EitherT.leftT[Id, Connection](NonEmptyList.one(InvalidGitHubUser(developers.first)))

    val body: String = """{"connected":false}"""
    val headers: Headers = Headers(`Content-Type`(MediaType.application.json), `Content-Length`(body.length))
    val response: Response[Id] = developers[Id, Json, Json](handler).apply(Request(uri = uri"/developers/connected/dev1/dev2"))

    response.status shouldBe Status.Ok
    response.headers shouldBe headers
    response.body.compile.toList shouldBe body.getBytes

  }

  it should "return the expected BadRequest status if at least an error happens" in {

    implicit val dsl: Http4sDsl[Id] = new Http4sDsl[Id] {}

    val handler: DevelopersHandler[Id] = developers =>
      EitherT.leftT[Id, Connection](NonEmptyList.one(InvalidGitHubUser(developers.first)))

    val body: String = """{"errors":["dev1 is not a valid user in github"]}"""
    val headers: Headers = Headers(`Content-Type`(MediaType.application.json), `Content-Length`(body.length))
    val response: Response[Id] = developers[Id, Json, Json](handler).apply(Request(uri = uri"/developers/connected/dev1/dev"))

    response.status shouldBe Status.BadRequest
    response.headers shouldBe headers
    response.body.compile.toList shouldBe body.getBytes

  }

  "notFound" should "return a function that always returns a NotFound response" in {

    implicit val dsl: Http4sDsl[Id] = new Http4sDsl[Id] {}

    val body: String = """{"errors":["missing resource"]}"""
    val headers: Headers = Headers(`Content-Type`(MediaType.application.json), `Content-Length`(body.length))
    val response: Response[Id] = notFound[Id, Json].apply(Request())

    response.status shouldBe Status.NotFound
    response.headers shouldBe headers
    response.body.compile.toList shouldBe body.getBytes

  }

}
