package com.github.aldtid.developers.connected.service.github

import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import com.github.aldtid.developers.connected.service.github.GitHubService._
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.error.{NotFound, UnexpectedResponse}
import com.github.aldtid.developers.connected.service.github.response.Organization

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import fs2.Stream
import io.circe.Json
import io.circe.generic.extras.auto._
import org.http4s.Header.Raw
import org.http4s.{Headers, Method, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIString
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger


class GitHubServiceTests extends AnyFlatSpec with Matchers {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger

  "getOrganizations" should "correctly decode an Ok response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/orgs")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Basic dXNlcm5hbWU6dG9rZW4="))

    val body: String = """[{"id":123,"login":"organization"}]"""
    val response: Response[IO] = Response(Status.Ok, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: GitHubConnection = GitHubConnection(baseUri, "username", "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getOrganizations[IO, Json]("user").unsafeRunSync() shouldBe Right(List(Organization("organization", 123)))

  }

  it should "handle a NotFound response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/orgs")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Basic dXNlcm5hbWU6dG9rZW4="))

    val body: String = """{"message":"Not Found","documentation_url":"url"}"""
    val response: Response[IO] = Response(Status.NotFound, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: GitHubConnection = GitHubConnection(baseUri, "username", "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getOrganizations[IO, Json]("user").unsafeRunSync() shouldBe Left(NotFound(body))

  }

  it should "handle an unexpected response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/orgs")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Basic dXNlcm5hbWU6dG9rZW4="))

    val body: String = """{"message":"Bad Request","documentation_url":"url"}"""
    val response: Response[IO] = Response(Status.BadRequest, body = Stream.iterable(body.getBytes))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: GitHubConnection = GitHubConnection(baseUri, "username", "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getOrganizations[IO, Json]("user").unsafeRunSync() shouldBe Left(UnexpectedResponse(400, body, None))

  }

  "bodyAs" should "decode a response body as expected" in {

    val body: String = """[{"login":"org","id":123}]"""
    val response: Response[IO] = Response(Status.Ok, body = Stream.iterable(body.getBytes))

    bodyAs[IO, List[Organization]](response).unsafeRunSync() shouldBe Right(List(Organization("org", 123)))

  }

  it should "return an UnexpectedResponse if body cannot be decoded" in {

    val response: Response[IO] = Response(Status.BadRequest, body = Stream())
    val either: Either[UnexpectedResponse, List[Organization]] = bodyAs[IO, List[Organization]](response).unsafeRunSync()

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
