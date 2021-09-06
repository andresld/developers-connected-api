package com.github.aldtid.developers.connected.service.github

import com.github.aldtid.developers.connected.service.github.GitHubService._
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.error.{DefaultError, NotFound, UnexpectedResponse}
import com.github.aldtid.developers.connected.service.github.response.Organization

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import fs2.Stream
import io.circe.generic.extras.auto._
import org.http4s.Header.Raw
import org.http4s.{Headers, Method, Request, Response, Status, Uri}
import org.http4s.client.Client
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.ci.CIString


class GitHubServiceTests extends AnyFlatSpec with Matchers {

  "getOrganizations" should "correctly decode an Ok response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/orgs")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Basic dXNlcm5hbWU6dG9rZW4="))

    val body: String = """[{"id":123,"login":"organization"}]"""
    val response: Response[IO] = Response(Status.Ok, body = Stream(body.getBytes: _*))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: GitHubConnection = GitHubConnection(baseUri, "username", "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getOrganizations[IO]("user").value.unsafeRunSync shouldBe Right(List(Organization("organization", 123)))

  }

  it should "correctly decode a NotFound response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/orgs")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Basic dXNlcm5hbWU6dG9rZW4="))

    val body: String = """{"message":"Not Found","documentation_url":"url"}"""
    val response: Response[IO] = Response(Status.NotFound, body = Stream(body.getBytes: _*))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: GitHubConnection = GitHubConnection(baseUri, "username", "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getOrganizations[IO]("user").value.unsafeRunSync shouldBe Left(NotFound("Not Found", "url"))

  }

  it should "correctly decode a BadRequest response as expected" in {

    val baseUri: Uri = Uri() / "root"

    val expectedUri: Uri = Uri.unsafeFromString("/root/users/user/orgs")
    val expectedHeaders: Headers = Headers(Raw(CIString("Authorization"), "Basic dXNlcm5hbWU6dG9rZW4="))

    val body: String = """{"message":"Bad Request","documentation_url":"url"}"""
    val response: Response[IO] = Response(Status.BadRequest, body = Stream(body.getBytes: _*))

    def behavior(request: Request[IO]): Resource[IO, Response[IO]] = {

      request.method shouldBe Method.GET
      request.uri shouldBe expectedUri
      request.headers shouldBe expectedHeaders

      Resource.pure(response)

    }

    implicit val connection: GitHubConnection = GitHubConnection(baseUri, "username", "token")
    implicit val client: Client[IO] = Client[IO](behavior)

    getOrganizations[IO]("user").value.unsafeRunSync shouldBe Left(DefaultError("Bad Request", "url"))

  }

  "bodyAs" should "decode a response body as expected" in {

    val body: String = """{"message":"Bad Request","documentation_url":"url"}"""
    val response: Response[IO] = Response(Status.BadRequest, body = Stream(body.getBytes: _*))

    bodyAs[IO, DefaultError](response).unsafeRunSync shouldBe Right(DefaultError("Bad Request", "url"))

  }

  it should "return an UnexpectedResponse if body cannot be decoded" in {

    val response: Response[IO] = Response(Status.BadRequest, body = Stream())
    val either: Either[UnexpectedResponse, DefaultError] = bodyAs[IO, DefaultError](response).unsafeRunSync

    either match {

      case Left(unexpectedResponse) =>
        unexpectedResponse.status shouldBe 400
        unexpectedResponse.body shouldBe ""
        unexpectedResponse.error.getMessage shouldBe "exhausted input"

      case right => fail(s"$right returned but a Left was expected")

    }

  }

}
