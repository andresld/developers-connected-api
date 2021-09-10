package com.github.aldtid.developers.connected.handler

import com.github.aldtid.developers.connected.logging.ProgramLog
import com.github.aldtid.developers.connected.logging.implicits.all._
import com.github.aldtid.developers.connected.logging.messages.{connectionErrors, connectionResult}
import com.github.aldtid.developers.connected.logging.tags.developersHandlerTag
import com.github.aldtid.developers.connected.model.Developers
import com.github.aldtid.developers.connected.model.responses._
import com.github.aldtid.developers.connected.service.github.GitHubService
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.{error => gerror}
import com.github.aldtid.developers.connected.service.github.response.Organization
import com.github.aldtid.developers.connected.service.twitter.TwitterService
import com.github.aldtid.developers.connected.service.twitter.connection.TwitterConnection
import com.github.aldtid.developers.connected.service.twitter.{error => terror}
import com.github.aldtid.developers.connected.service.twitter.response.{Followers, User}

import cats.data.{EitherT, NonEmptyList}
import cats.effect.{Clock, Concurrent}
import cats.effect.implicits._
import cats.effect.kernel.Outcome
import org.http4s.client.Client
import org.typelevel.log4cats.Logger


/**
 * Defines the behaviour for 'developers' routes.
 *
 * Each function represents a handling function for one endpoint. Functions do not expose extra parameters (connections
 * to external services, logging instances...) or types (logging) as later router functions may get overloaded with
 * multiple parameters, making them harder to test and to deal with. Because of that, those instances (if required) must
 * be offered at handler creation time.
 *
 * @tparam F context type
 */
trait DevelopersHandler[F[_]] {

  /**
   * Defines the handling for checking the connection between two developers.
   *
   * @param developers developers to check the connection between
   * @return the existing or non-existent connection, or an error otherwise
   */
  def checkConnection(developers: Developers): EitherT[F, Errors, Connection]

}

object DevelopersHandler {

  // Type alias to simplify and make clearer some arguments types
  type OutEither[F[_], A] = Outcome[EitherT[F, NonEmptyList[Error], *], Throwable, A]

  /**
   * Creates an EitherT instance from passed [[Outcome]], returning a default [[InterruptedExecution]] in case the
   * execution was cancelled for the outcome.
   *
   * @param out outcome to handle
   * @tparam F context type
   * @tparam A either value
   * @return the resulting EitherT from the outcome
   */
  def embed[F[_] : Concurrent, A](out: OutEither[F, A]): EitherT[F, NonEmptyList[Error], A] =
    out.embed(EitherT.leftT(NonEmptyList.one(InterruptedExecution)))

  /**
   * Takes two outcomes and joins them following this schema:
   *   - if both outcomes result in a Left value, the result will be both error lists concatenated as a Left
   *   - if some of the outcomes result in a Left value, the result will be the error list as a Left
   *   - if both outcomes result in a Right value, the result will be the tupled values as a Right
   *
   * @tparam F context type
   * @tparam A1 first outcome value type
   * @tparam A2 second outcome value type
   * @return the resulting error(s) or tupled values depending on outcomes result
   */
  def join[F[_] : Concurrent, A1, A2]: ((OutEither[F, A1], OutEither[F, A2])) => EitherT[F, NonEmptyList[Error], (A1, A2)] = {

    case (out1, out2) =>
      embed(out1).biflatMap[NonEmptyList[Error], (A1, A2)](
        e1 => embed(out2).biflatMap(e2 => EitherT.leftT(e1 ::: e2), _ => EitherT.leftT(e1)),
        a1 => embed(out2).biflatMap(e2 => EitherT.leftT(e2), a2 => EitherT.rightT((a1, a2))),
      )

  }

  /**
   * Runs two EitherT instances in parallel, making them wait the one for the other to continue the execution and, then,
   * applying the passed function in case the execution was successful.
   *
   * @param first first either
   * @param second second either
   * @param apply function to apply
   * @tparam F context type
   * @tparam A1 first either value type
   * @tparam A2 second either value type
   * @tparam B resulting type of applying the function
   * @return a B instance if executions were successful or a list of errors otherwise
   */
  def parallel[F[_] : Concurrent, A1, A2, B](first: EitherT[F, NonEmptyList[Error], A1],
                                             second: EitherT[F, NonEmptyList[Error], A2],
                                             apply: (A1, A2) => B): EitherT[F, NonEmptyList[Error], B] =
    first.bothOutcome(second).flatMap(join).map(tuple => apply(tuple._1, tuple._2))

  /**
   * Defines the followers retrieve for a Twitter username.
   *
   * First of all, the Twitter user for passed username is retrieved, if possible. In case of succeed, then the
   * followers for that user are retrieved, if any. In case any error happens, it is returned.
   *
   * @param username twitter username to retrieve the followers for
   * @param twitter twitter service
   * @param twConnection twitter connection details
   * @tparam F context type
   * @tparam L logging type to format
   * @return the username followers or an error otherwise
   */
  def getFollowers[F[_] : Concurrent : Clock : Client : Logger,
                   L : ProgramLog](username: String, twitter: TwitterService[F])
                                  (implicit twConnection: TwitterConnection): EitherT[F, Error, Followers] =
    twitter.getUserByUsername(username)
      .leftMap({
        case terror.BadRequest(_) => InvalidTwitterUser(username)
        case error                => InternalTwitterError(username, error)
      })
      .flatMap(
        _.data.fold[EitherT[F, Error, Followers]](EitherT.leftT(InvalidTwitterUser(username)))(data =>
          twitter.getUserFollowers(data.id).leftMap(InternalTwitterError(username, _))
        )
      )

  /**
   * Defines the organizations retrieve for a GitHub username.
   *
   * In case any error happens, it is returned.
   *
   * @param username github username
   * @param github github service
   * @param ghConnection github connection details
   * @tparam F context type
   * @tparam L logging type to format
   * @return the username organizations or a list of a single error otherwise
   */
  def getOrganizations[F[_] : Concurrent : Clock : Client : Logger,
                       L : ProgramLog](username: String, github: GitHubService[F])
                                      (implicit ghConnection: GitHubConnection): EitherT[F, Error, List[Organization]] =
    github.getOrganizations(username)
      .leftMap({
        case gerror.NotFound(_) => InvalidGitHubUser(username)
        case error              => InternalGitHubError(username, error)
      })

  /**
   * Performs the check of two developer usernames connection.
   *
   * This connection follows next rules:
   *   - both developers must exist in Twitter and GitHub
   *   - both developers must follow each other in Twitter
   *   - both developers must have at least one organization in common in GitHub
   *
   * Because of above rules, the flow focuses on three main steps: Twitter data extraction, GitHub data extraction and
   * connection checking. The checking must be performed once both extractions have finished, but in the data extraction
   * case there is nothing that impedes to be executed in a certain order. Having that in mind, there are three levels
   * or parallelism on this flow:
   *   - Twitter followers are extracted concurrently
   *   - GitHub organizations are extracted concurrently
   *   - Twitter and GitHub extractions are performed concurrently
   *
   * Having this concurrency level allows the handling to be faster to resolve the checking computation.
   *
   * In case errors happen during this execution, they are aggregated in a non-empty list and returned.
   *
   * @param github github service
   * @param twitter twitter service
   * @param developers developers to check
   * @param pl logging instances
   * @param ghConnection github connection details
   * @param twConnection twitter connection details
   * @tparam F context type
   * @tparam L logging type to format
   * @return the connection result or a list of errors otherwise
   */
  def checkConnection[F[_] : Concurrent : Clock : Client : Logger, L](github: GitHubService[F],
                                                                      twitter: TwitterService[F],
                                                                      developers: Developers)
                                                                     (implicit pl: ProgramLog[L],
                                                                      ghConnection: GitHubConnection,
                                                                      twConnection: TwitterConnection): EitherT[F, Errors, Connection] = {

    import pl._

    val parallelOrganizations: EitherT[F, NonEmptyList[Error], List[Organization]] =
      parallel[F, List[Organization], List[Organization], List[Organization]](
        getOrganizations(developers.first, github).leftMap(NonEmptyList.one),
        getOrganizations(developers.second, github).leftMap(NonEmptyList.one),
        (orgs1, orgs2) => orgs1.filter(orgs2.contains)
      )

    val parallelUsers: EitherT[F, NonEmptyList[Error], List[User]] =
      parallel[F, Followers, Followers, List[User]](
        getFollowers(developers.first, twitter).leftMap(NonEmptyList.one),
        getFollowers(developers.second, twitter).leftMap(NonEmptyList.one),
        (fol1, fol2) => fol1.data.zip(fol2.data).map(tuple => tuple._1.filter(tuple._2.contains)).getOrElse(Nil)
      )

    parallel[F, List[Organization], List[User], Connection](
      parallelOrganizations,
      parallelUsers,
      (orgs, users) =>
        if (orgs.isEmpty || users.isEmpty) NotConnected
        else Connected(NonEmptyList.fromListUnsafe(orgs.map(_.login)))
    )
      .leftMap(Errors(_))
      .semiflatTap(connection => Logger[F].info(connectionResult |+| connection |+| developersHandlerTag))
      .leftSemiflatTap(errors => Logger[F].error(connectionErrors |+| errors |+| developersHandlerTag))

  }

  /**
   * Default implementation for 'developers' endpoints handler.
   *
   * @param github github service
   * @param twitter twitter service
   * @param ghConnection github connection details
   * @param twConnection twitter connection details
   * @tparam F context type
   * @tparam L logging type to format
   * @return the default handler implementation
   */
  def default[F[_] : Concurrent : Clock : Client : Logger, L : ProgramLog](github: GitHubService[F],
                                                                           twitter: TwitterService[F])
                                                                          (implicit ghConnection: GitHubConnection,
                                                                           twConnection: TwitterConnection): DevelopersHandler[F] =
    (developers: Developers) => DevelopersHandler.checkConnection(github, twitter, developers)

}
