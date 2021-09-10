package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.configuration._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s.implicits.http4sLiteralsSyntax
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ConfigurationTests extends AnyFlatSpec with Matchers {

  "loadConfiguration" should "correctly load a configuration from the file system" in {

    loadConfiguration[IO].unsafeRunSync() shouldBe
      Right(
        Configuration(
          Server("localhost", 8080, "/"),
          GitHub(uri"http://localhost:12345", "developer", "github-token"),
          Twitter(uri"http://localhost:23456", "twitter-token"),
          ThreadPools(10, 10),
          Cache(60)
        )
      )

  }

}
