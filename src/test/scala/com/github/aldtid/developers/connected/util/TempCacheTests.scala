package com.github.aldtid.developers.connected.util

import com.github.aldtid.developers.connected.util.TempCache._
import cats.Applicative
import cats.effect.unsafe.implicits.global
import cats.effect.{Clock, IO}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._


class TempCacheTests extends AnyFlatSpec with Matchers {

  def clock[F[_] : Applicative](duration: FiniteDuration): Clock[F] = new Clock[F] {

    def applicative: Applicative[F] = Applicative[F]

    def monotonic: F[FiniteDuration] = Applicative[F].pure(duration)

    def realTime: F[FiniteDuration] = Applicative[F].pure(duration)

  }

  "set" should "save a value in the cache with expected expiration time" in {

    implicit val clk: Clock[IO] = clock(5.seconds)

    (
      for {
        cache <- createCache[IO, String, String]
        _     <- set("key", "value", 5.seconds, cache)
        map   <- cache.get
      } yield map
    ).unsafeRunSync() shouldBe Map("key" -> CacheValue("value", 10.seconds))

  }

  "get" should "return the contained value if it has not expired yet" in {

    implicit val clk: Clock[IO] = clock(0.seconds)

    (
      for {
        cache  <- createCache[IO, String, String]
        _      <- cache.update(_ + ("key" -> CacheValue("value", 10.seconds)))
        option <- get("key", cache)
        map    <- cache.get
      } yield (option, map)
      ).unsafeRunSync() shouldBe (Some("value"), Map("key" -> CacheValue("value", 10.seconds)))

  }

  it should "return an option and clean the cache if the value expired" in {

    implicit val clk: Clock[IO] = clock(11.seconds)

    (
      for {
        cache  <- createCache[IO, String, String]
        _      <- cache.update(_ + ("key" -> CacheValue("value", 10.seconds)))
        option <- get("key", cache)
        map    <- cache.get
      } yield (option, map)
      ).unsafeRunSync() shouldBe (None, Map())

  }

  it should "return a None if the key is not present" in {

    implicit val clk: Clock[IO] = clock(0.seconds)

    (
      for {
        cache  <- createCache[IO, String, String]
        option <- get("key", cache)
        map    <- cache.get
      } yield (option, map)
      ).unsafeRunSync() shouldBe (None, Map())

  }

}
