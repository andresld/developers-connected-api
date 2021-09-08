package com.github.aldtid.developers.connected.service

import com.github.aldtid.developers.connected.logging.Log
import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import com.github.aldtid.developers.connected.service.util._

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import fs2.Stream
import io.circe.{Error, Json}
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{Method, Request, Response, Status, Uri}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class UtilsTests extends AnyFlatSpec with Matchers {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger

  "requestWithLogs" should "apply passed function after the request retrieved a response" in {

    val log: Log[Json] = jsonProgramLog.log
    val expectedUri: Uri = uri"/"
    val response: Response[IO] = Response(Status.Ok)

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri

      Resource.pure(response)

    }

    def handle(response: Response[IO]): IO[Int] = IO(response.status.code)

    implicit val client: Client[IO] = Client[IO](behavior)

    requestWithLogs[IO, Json, Int](Request(), log, log, handle).unsafeRunSync() shouldBe 200

  }

  "bodyAs" should "decode a response body as expected" in {

    val body: String = "1"
    val response: Response[IO] = Response(Status.BadRequest, body = Stream.iterable(body.getBytes))

    bodyAs[IO, String, Int](response, (_, body, _) => body).unsafeRunSync() shouldBe Right(1)

  }

  it should "return an UnexpectedResponse if body cannot be decoded" in {

    val response: Response[IO] = Response(Status.BadRequest, body = Stream())
    val either: Either[(Int, String, Error), Int] =
      bodyAs[IO, (Int, String, Error), Int](response, (i, b, e) => (i, b, e)).unsafeRunSync()

    either match {

      case Left((status, body, error)) =>
        status shouldBe 400
        body shouldBe ""
        error.getMessage shouldBe "exhausted input"

      case right => fail(s"$right returned but a Left was expected")

    }

  }

  "bodyAs" should "extract a response body as String and apply the passed function" in {

    val body: String = "1"
    val response: Response[IO] = Response(Status.BadRequest, body = Stream.iterable(body.getBytes))

    bodyAs[IO, String](response, (_, body) => s"handled: $body").unsafeRunSync() shouldBe "handled: 1"

  }

}
