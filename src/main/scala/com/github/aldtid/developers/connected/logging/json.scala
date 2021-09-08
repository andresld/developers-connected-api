package com.github.aldtid.developers.connected.logging

import com.github.aldtid.developers.connected.configuration.Server
import com.github.aldtid.developers.connected.logging.Log.createLog
import com.github.aldtid.developers.connected.logging.model._
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.response.Organization
import com.github.aldtid.developers.connected.service.github.{error => gerror}
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.service.twitter.response.{Followers, UserData}
import com.github.aldtid.developers.connected.service.twitter.{error => terror}
import io.circe.{Encoder, Json, Printer, Error => CError}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.{Header, Headers, Request, Response, Uri}
import pureconfig.error.ConfigReaderFailures

import java.io.{PrintWriter, StringWriter}


/**
 * Json representation for program logs.
 */
object json {

  // ----- BASE LOG INSTANCE -----
  val jsonLog: Log[Json] = createLog[Json](Json.obj(), _.printWith(Printer.noSpaces))((x, y) => y deepMerge x)
  // ----------

  // ----- LOGGABLE INSTANCES -----
  def jsonRequestLoggable[F[_]]: Loggable[Request[F], Json] = request =>
    Json.obj(
      "request" -> Json.obj(
        "method" -> Json.fromString(request.method.name),
        "uri" -> Json.fromString(request.uri.renderString),
        "version" -> Json.fromString(request.httpVersion.renderString),
        "headers" -> toJson(request.headers)
      )
    )

  def jsonResponseLoggable[F[_]]: Loggable[Response[F], Json] = response =>
    Json.obj(
      "response"-> Json.obj(
        "status" -> Json.fromInt(response.status.code),
        "version"-> Json.fromString(response.httpVersion.renderString),
        "headers" -> toJson(response.headers)
      )
    )

  val jsonConfigReaderFailuresLoggable: Loggable[ConfigReaderFailures, Json] =
    failures => Json.obj("failures" -> failures.prettyPrint().filterNot(_ == '\r').asJson)

  val jsonMessageLoggable: Loggable[Message, Json] = _.asJson
  val jsonTagLoggable: Loggable[Tag, Json] = _.asJson
  val jsonUsernameLoggable: Loggable[Username, Json] = _.asJson
  val jsonIdentifierLoggable: Loggable[Identifier, Json] = _.asJson
  val jsonLatencyLoggable: Loggable[Latency, Json] = _.asJson
  val jsonThreadPoolLoggable: Loggable[ThreadPool, Json] = _.asJson

  val jsonConfigurationServerLoggable: Loggable[Server, Json] =
    server => Json.obj("server" -> server.asJson)

  val jsonTwitterUserDataLoggable: Loggable[UserData, Json] =
    data => Json.obj("user" -> data.data.asJson)

  val jsonTwitterFollowersLoggable: Loggable[Followers, Json] =
    followers => Json.obj("followers" -> followers.data.asJson, "meta" -> followers.meta.asJson)

  val jsonTwitterErrorLoggable: Loggable[terror.Error, Json] =
    error => wrapError(jsonTwitterError(error))

  val jsonTwitterError: terror.Error => Json = {
    case error: terror.NotFound => Json.obj("notFound" -> error.asJson)
    case error: terror.UnexpectedResponse => Json.obj("unexpected" -> error.asJson)
  }

  val jsonTwitterConnectionLoggable: Loggable[TwitterConnection, Json] =
    connection =>
      Json.obj(
        "twitter" -> Json.obj(
          "uri" -> connection.baseUri.asJson
        )
      )

  val jsonGithubOrganizationsLoggable: Loggable[List[Organization], Json] =
    list => Json.obj("organizations" -> Json.arr(list.map(_.asJson): _*))

  val jsonGithubErrorLoggable: Loggable[gerror.Error, Json] =
    error => wrapError(jsonGithubError(error))

  val jsonGithubError: gerror.Error => Json = {
    case error: gerror.NotFound => Json.obj("notFound" -> error.asJson)
    case error: gerror.UnexpectedResponse => Json.obj("unexpected" -> error.asJson)
  }

  val jsonGithubConnectionLoggable: Loggable[GitHubConnection, Json] =
    connection =>
      Json.obj(
        "github" -> Json.obj(
          "uri" -> connection.baseUri.asJson,
          "username" -> connection.username.asJson
        )
      )
  // ----------

  // ----- UTILITY FUNCTIONS -----
  def toJson(headers: Headers): Json = headers.headers.map(toJson).foldLeft(Json.arr())(_ deepMerge _)

  def toJson(raw: Header.Raw): Json = Json.obj(raw.name.toString -> Json.fromString(raw.value))

  def wrapError(json: Json): Json = Json.obj("error" -> json)

  def stackTraceToString(throwable: Throwable): String = {

    val writer: StringWriter = new StringWriter()
    val printer: PrintWriter = new PrintWriter(writer)

    throwable.printStackTrace(printer)
    writer.toString

  }
  // ----------

  // ----- CIRCE ENCODERS -----
  implicit val uriEncoder: Encoder[Uri] = _.renderString.asJson

  implicit val circeErrorEncoder: Encoder[CError] = (_: Throwable).asJson

  implicit val exceptionEncoder: Encoder[Throwable] =
    throwable =>
      Json.obj(
        "class" -> throwable.getClass.getName.asJson,
        "message" -> throwable.getMessage.asJson,
        "trace" -> stackTraceToString(throwable).asJson,
        "cause" -> Option(throwable.getCause).asJson
      )
  // ----------

  implicit val jsonProgramLog: ProgramLog[Json] = new ProgramLog[Json] {

    implicit val log: Log[Json] = jsonLog

    implicit def requestLoggable[F[_]]: Loggable[Request[F], Json] = jsonRequestLoggable
    implicit def responseLoggable[F[_]]: Loggable[Response[F], Json] = jsonResponseLoggable

    implicit val configReaderFailuresLoggable: Loggable[ConfigReaderFailures, Json] = jsonConfigReaderFailuresLoggable

    implicit val messageLoggable: Loggable[Message, Json] = jsonMessageLoggable
    implicit val tagLoggable: Loggable[Tag, Json] = jsonTagLoggable
    implicit val usernameLoggable: Loggable[Username, Json] = jsonUsernameLoggable
    implicit val identifierLoggable: Loggable[Identifier, Json] = jsonIdentifierLoggable
    implicit val latencyLoggable: Loggable[Latency, Json] = jsonLatencyLoggable
    implicit val threadPoolLoggable: Loggable[ThreadPool, Json] = jsonThreadPoolLoggable

    implicit val configurationServerLoggable: Loggable[Server, Json] = jsonConfigurationServerLoggable

    implicit val twitterUserDataLoggable: Loggable[UserData, Json] = jsonTwitterUserDataLoggable
    implicit val twitterFollowersLoggable: Loggable[Followers, Json] = jsonTwitterFollowersLoggable
    implicit val twitterErrorLoggable: Loggable[terror.Error, Json] = jsonTwitterErrorLoggable
    implicit val twitterConnectionLoggable: Loggable[TwitterConnection, Json] = jsonTwitterConnectionLoggable

    implicit val githubOrganizationsLoggable: Loggable[List[Organization], Json] = jsonGithubOrganizationsLoggable
    implicit val githubErrorLoggable: Loggable[gerror.Error, Json] = jsonGithubErrorLoggable
    implicit val githubConnectionLoggable: Loggable[GitHubConnection, Json] = jsonGithubConnectionLoggable

  }

}
