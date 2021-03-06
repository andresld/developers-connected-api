package com.github.aldtid.developers.connected

import cats.effect.Sync
import cats.implicits._
import org.http4s.Uri
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.{CannotConvert, ConfigReaderFailures}
import pureconfig.generic.auto._


object configuration {

  import implicits._

  final case class Configuration(server: Server, github: GitHub, twitter: Twitter, threadPools: ThreadPools, cache: Cache)

  final case class Server(host: String, port: Int, basePath: String)

  final case class GitHub(host: Uri, username: String, token: String)

  final case class Twitter(host: Uri, token: String)

  final case class ThreadPools(server: Int, client: Int)

  final case class Cache(timeoutSeconds: Long)

  /**
   * Reads a configuration file from the file system and tries to parse it.
   *
   * @tparam F context type
   * @return the loaded configuration or a reading failure
   */
  def loadConfiguration[F[_] : Sync]: F[Either[ConfigReaderFailures, Configuration]] =
    Sync[F].delay(ConfigSource.default.load[Configuration])

  object implicits {

    implicit val uriConfigReader: ConfigReader[Uri] =
      ConfigReader.fromString(string =>
        Uri.fromString(string).leftMap(error => CannotConvert(string, "Uri", error.sanitized))
      )

  }

}
