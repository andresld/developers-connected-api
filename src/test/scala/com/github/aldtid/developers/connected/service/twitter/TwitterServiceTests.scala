package com.github.aldtid.developers.connected.service.twitter

import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import com.github.aldtid.developers.connected.service.twitter.TwitterService._
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.service.twitter.error.{NotFound, UnexpectedResponse}
import com.github.aldtid.developers.connected.service.twitter.response._

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import fs2.Stream
import io.circe.Json
import io.circe.generic.extras.auto._
import org.http4s.Header.Raw
import org.http4s.client.Client
import org.http4s._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class TwitterServiceTests extends AnyFlatSpec with Matchers {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger

  "getUserByUsername" should "correctly decode an Ok response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/by/username/user")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Bearer token"))

    val body: String = """{"data":{"id":"123","name":"user-name","username":"user-username"}}"""
    val response: Response[IO] = Response(Status.Ok, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: TwitterConnection = TwitterConnection(baseUri, "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getUserByUsername[IO, Json]("user").unsafeRunSync() shouldBe Right(UserData(User("123","user-name", "user-username")))

  }

  it should "handle a NotFound response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/by/username/user")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Bearer token"))

    val body: String = """{"error":"some error"}"""
    val response: Response[IO] = Response(Status.NotFound, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: TwitterConnection = TwitterConnection(baseUri, "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getUserByUsername[IO, Json]("user").unsafeRunSync() shouldBe Left(NotFound(body))

  }

  it should "handle an unexpected response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/by/username/user")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Bearer token"))

    val body: String = """{error":"some error"}"""
    val response: Response[IO] = Response(Status.BadRequest, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: TwitterConnection = TwitterConnection(baseUri, "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getUserByUsername[IO, Json]("user").unsafeRunSync() shouldBe Left(UnexpectedResponse(400, body, None))

  }

  "getUserFollowers" should "correctly decode an Ok response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/followers")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Bearer token"))

    val body: String = """{"data":[{"id":"123","name":"user-name","username":"user-username"}],"meta":{"result_count":1}}"""
    val response: Response[IO] = Response(Status.Ok, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: TwitterConnection = TwitterConnection(baseUri, "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getUserFollowers[IO, Json]("user").unsafeRunSync() shouldBe
      Right(Followers(Some(List(User("123","user-name", "user-username"))), Meta(1)))

  }

  it should "handle an unexpected response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/followers")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Bearer token"))

    val body: String = """{"error":"some error"}"""
    val response: Response[IO] = Response(Status.BadRequest, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: TwitterConnection = TwitterConnection(baseUri, "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getUserFollowers[IO, Json]("user").unsafeRunSync() shouldBe Left(UnexpectedResponse(400, body, None))

  }

  "bodyAs" should "decode a response body as expected" in {

    val body: String = """{"data":{"id":"123","name":"user-name","username":"user-username"}}"""
    val response: Response[IO] = Response(Status.Ok, body = Stream.iterable(body.getBytes))

    bodyAs[IO, UserData](response).unsafeRunSync() shouldBe Right(UserData(User("123", "user-name", "user-username")))

  }

  it should "return an UnexpectedResponse if body cannot be decoded" in {

    val response: Response[IO] = Response(Status.BadRequest, body = Stream())
    val either: Either[UnexpectedResponse, UserData] = bodyAs[IO, UserData](response).unsafeRunSync()

    either match {

      case Left(unexpectedResponse) =>
        unexpectedResponse.status shouldBe 400
        unexpectedResponse.body shouldBe ""

        unexpectedResponse.error match {

          case Some(error) => error.getMessage shouldBe "exhausted input"
          case None => fail("error should not be a None")

        }

      case right => fail(s"$right returned but a Left was expected")

    }

  }

}
