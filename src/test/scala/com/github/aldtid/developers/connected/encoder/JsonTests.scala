package com.github.aldtid.developers.connected.encoder

import com.github.aldtid.developers.connected.encoder.json.jsonResponseEncoder
import com.github.aldtid.developers.connected.model.responses._
import com.github.aldtid.developers.connected.service.github.{error => gerror}
import com.github.aldtid.developers.connected.service.twitter.{error => terror}

import cats.Id
import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class JsonTests extends AnyFlatSpec with Matchers {

  "jsonResponseEncoder" should "contain the expected entity encoder" in {

    jsonResponseEncoder.entityEncoder[Id].toEntity(Json.obj()).body.compile.toList shouldBe List(123.toByte, 125.toByte)

  }

  it should "contain the expected encoder for a Connection instance" in {

    jsonResponseEncoder.connectionEncoder.encode(NotConnected) shouldBe
      Json.obj("connected" -> Json.fromBoolean(false))

    jsonResponseEncoder.connectionEncoder.encode(Connected.of("org1")) shouldBe
      Json.obj("connected" -> Json.fromBoolean(true), "organizations" -> Json.arr(Json.fromString("org1")))

  }

  it should "contain the expected encoder for an Errors instance" in {

    val errors: Errors = Errors.of(
      InvalidGitHubUser("dev1"),
      InvalidTwitterUser("dev2"),
      InternalGitHubError("dev3", gerror.UnexpectedResponse(400, "body", None)),
      InternalTwitterError("dev4", terror.UnexpectedResponse(400, "body", None)),
      MissingResource,
      InterruptedExecution
    )

    jsonResponseEncoder.errorsEncoder.encode(errors) shouldBe
        Json.obj(
          "errors" -> Json.arr(
            Json.fromString("dev1 is not a valid user in github"),
            Json.fromString("dev2 is not a valid user in twitter"),
            Json.fromString("internal error with dev3 in github"),
            Json.fromString("internal error with dev4 in twitter"),
            Json.fromString("missing resource"),
            Json.fromString("interrupted execution")
          )
        )

  }

}
