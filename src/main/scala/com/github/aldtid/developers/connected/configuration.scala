package com.github.aldtid.developers.connected

import cats.effect.Sync
import cats.implicits._
import org.http4s.Uri
import pureconfig.{ConfigReader, ConfigSource}
import pureconfig.error.{CannotConvert, ConfigReaderFailures}
import pureconfig.generic.auto._


object configuration {

  final case class Configuration(server: Server, github: GitHub, twitter: Twitter, threadPools: ThreadPools)

  final case class Server(host: String, port: Int, basePath: String)

  final case class GitHub(host: Uri, username: String, token: String)

  final case class Twitter(host: Uri, token: String)

  final case class ThreadPools(server: Int, client: Int)

  implicit val uriConfigReader: ConfigReader[Uri] =
    ConfigReader.fromString(string =>
      Uri.fromString(string).leftMap(error => CannotConvert(string, "Uri", error.sanitized))
    )

  def loadConfiguration[F[_] : Sync]: F[Either[ConfigReaderFailures, Configuration]] =
    Sync[F].delay(ConfigSource.default.load[Configuration])

}
