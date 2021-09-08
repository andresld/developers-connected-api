package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.configuration._
import com.github.aldtid.developers.connected.encoder.BodyEncoder
import com.github.aldtid.developers.connected.handler.DevelopersHandler
import com.github.aldtid.developers.connected.logging.{Log, ProgramLog}
import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages._
import com.github.aldtid.developers.connected.logging.tags._
import com.github.aldtid.developers.connected.service.github.GitHubService
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.twitter.TwitterService
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection

import cats.effect.{ExitCode, Sync}
import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}


object launcher {

  /**
   * Tries to load a configuration from the file system.
   *
   * In case the load succeeds, a function is applied for parsed configuration, otherwise the exit code is a failure.
   * Logs are shown before and after loading the configuration.
   *
   * @param f function to apply if the load succeeds
   * @param pl logging instances
   * @tparam F context type
   * @tparam L logging type to format
   * @return the resulting exit code of applying passed function or a failed configuration load
   */
  def handleConfiguration[F[_] : Sync : Logger, L](f: Configuration => F[ExitCode])
                                                  (implicit pl: ProgramLog[L]): F[ExitCode] = {

    import pl._

    val baseLog: Log[L] = launcherTag

    Logger[F].info(baseLog |+| loadingConfiguration) *> loadConfiguration.flatMap({

      case Right(configuration) => Logger[F].info(baseLog |+| configurationLoaded) *> f(configuration)
      case Left(errors)         => Logger[F].info(baseLog |+| configurationErrors |+| errors).as(ExitCode.Error)

    })

  }

  /**
   * Creates a fixed thread pool with passed size.
   *
   * @param size thread pool size
   * @tparam F context type
   * @return the thread pool
   */
  def threadPool[F[_]: Sync](size: Int): F[ExecutionContextExecutorService] =
    Sync[F].delay(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(size)))

  /**
   * Prepares the environment to run the server and starts it.
   *
   * The environment steps to prepare before the server run are:
   *   - the configuration load
   *   - thread pools creation
   *   - endpoint handler
   *   - a HTTP client to perform requests
   *
   * If the configuration load fails, the application will not be started.
   *
   * @tparam F context type
   * @tparam L logging type to format
   * @tparam O body type to encode
   * @return
   */
  def prepareAndStart[F[_] : Async : Http4sDsl : Logger, L, O : BodyEncoder](implicit pl: ProgramLog[L]): F[ExitCode] = {

    import pl._

    val baseLog: Log[L] = launcherTag

    def threadPoolsAndRun(configuration: Configuration): F[ExitCode] =
      for {

        _        <- Logger[F].info(baseLog |+| clientThreadPool |+| configuration.threadPools.client.asThreadPool)
        clientEC <- threadPool(configuration.threadPools.client)
        _        <- Logger[F].info(baseLog |+| serverThreadPool |+| configuration.threadPools.client.asThreadPool)
        serverEC <- threadPool(configuration.threadPools.server)
        code     <- run(configuration.github, configuration.twitter, configuration.server, clientEC, serverEC)

      } yield code

    def run(github: GitHub, twitter: Twitter, server: Server, clientEC: ExecutionContext, serverEC: ExecutionContext): F[ExitCode] =
      Logger[F].info(baseLog |+| creatingClient) *>
        BlazeClientBuilder[F](clientEC).resource.use(implicit client => {

          implicit val ghConnection: GitHubConnection = GitHubConnection(github.host, github.username, github.token)
          implicit val twConnection: TwitterConnection = TwitterConnection(twitter.host, twitter.token)

          for {

            _          <- Logger[F].info(baseLog |+| githubConnection |+| ghConnection)
            _          <- Logger[F].info(baseLog |+| twitterConnection |+| twConnection)
            _          <- Logger[F].info(baseLog |+| startingServer |+| server)
            devHandler  = DevelopersHandler.default[F](GitHubService.default, TwitterService.default)
            code       <- start[F, L, O](serverEC, server, devHandler)

          } yield code

        })

    handleConfiguration(threadPoolsAndRun)

  }

  /**
   * Starts a server with passed thread pool, server configuration and endpoints handler.
   *
   * @param ec server thread pool
   * @param server server configuration
   * @param developersHandler developer endpoints handler
   * @param ghConnection github connection
   * @param twConnection twitter connection
   * @tparam F context type
   * @tparam L logging type to format
   * @tparam O body type to encode
   * @return the exit code for the server
   */
  def start[F[_] : Async : Http4sDsl : Client : Logger, L : ProgramLog, O : BodyEncoder](ec: ExecutionContext,
                                                                                         server: Server,
                                                                                         developersHandler: DevelopersHandler[F])
                                                                                        (implicit ghConnection: GitHubConnection,
                                                                                         twConnection: TwitterConnection): F[ExitCode] =

    BlazeServerBuilder[F](ec)
      .bindHttp(server.port, server.host)
      .withHttpApp(application.app(server.basePath, developersHandler))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}
