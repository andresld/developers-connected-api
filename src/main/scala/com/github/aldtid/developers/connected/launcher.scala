package com.github.aldtid.developers.connected

import com.github.aldtid.developers.connected.configuration._
import com.github.aldtid.developers.connected.encoder.BodyEncoder
import com.github.aldtid.developers.connected.handler.DevelopersHandler
import com.github.aldtid.developers.connected.handler.DevelopersHandler.{Cache, UserFollowers}
import com.github.aldtid.developers.connected.logging.{Log, ProgramLog}
import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages._
import com.github.aldtid.developers.connected.logging.tags._
import com.github.aldtid.developers.connected.model.responses.Error
import com.github.aldtid.developers.connected.service.github.GitHubService
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.response.Organization
import com.github.aldtid.developers.connected.service.twitter.TwitterService
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.util.TempCache

import cats.data.NonEmptyList
import cats.effect.{ExitCode, Sync}
import cats.effect.kernel.Async
import cats.implicits._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import pureconfig.error.ConfigReaderFailures

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.concurrent.duration._


object launcher {

  /**
   * Tries to load a configuration from the file system using passed effect.
   *
   * In case the load succeeds, a function is applied for parsed configuration, otherwise the exit code is a failure.
   * Logs are shown before and after loading the configuration.
   *
   * @param eitherF effect for configuration load result
   * @param onSuccess function to apply if the load succeeds
   * @param pl logging instances
   * @tparam F context type
   * @tparam L logging type to format
   * @return the resulting exit code of applying passed function or a failed configuration load
   */
  def handleConfiguration[F[_] : Sync : Logger, L](eitherF: F[Either[ConfigReaderFailures, Configuration]],
                                                   onSuccess: Configuration => F[ExitCode])
                                                  (implicit pl: ProgramLog[L]): F[ExitCode] = {

    import pl._

    val baseLog: Log[L] = launcherTag

    Logger[F].info(baseLog |+| loadingConfiguration) *> eitherF.flatMap({

      case Right(configuration) => Logger[F].info(baseLog |+| configurationLoaded) *> onSuccess(configuration)
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
        code     <- run(configuration, clientEC, serverEC)

      } yield code

    def run(config: Configuration, clientEC: ExecutionContext, serverEC: ExecutionContext): F[ExitCode] =
      Logger[F].info(baseLog |+| creatingClient) *>
        BlazeClientBuilder[F](clientEC).resource.use(implicit client => {

          implicit val ghConnection: GitHubConnection = GitHubConnection(config.github.host, config.github.username, config.github.token)
          implicit val twConnection: TwitterConnection = TwitterConnection(config.twitter.host, config.twitter.token)

          val timeout: FiniteDuration = config.cache.timeoutSeconds.seconds

          def handler(orgsCache: Cache[F, String, List[Organization]], folCache: Cache[F, String, UserFollowers]): DevelopersHandler[F] =
            DevelopersHandler.default[F, L](GitHubService.default, TwitterService.default, orgsCache, folCache, timeout)

          for {

            _          <- Logger[F].info(baseLog |+| githubConnection |+| ghConnection)
            _          <- Logger[F].info(baseLog |+| twitterConnection |+| twConnection)
            _          <- Logger[F].info(baseLog |+| startingServer |+| config.server)

            orgsCache  <- TempCache.createCache[F, String, Either[NonEmptyList[Error], List[Organization]]]
            folCache   <- TempCache.createCache[F, String, Either[NonEmptyList[Error], UserFollowers]]
            devHandler  = handler(TempCache.default(orgsCache), TempCache.default(folCache))

            code       <- start[F, L, O](serverEC, config.server, devHandler)

          } yield code

        })

    handleConfiguration(loadConfiguration, threadPoolsAndRun)

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
