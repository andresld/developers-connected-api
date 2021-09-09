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


trait DevelopersHandler[F[_]] {

  def checkConnection(developers: Developers): EitherT[F, Errors, Connection]

}

object DevelopersHandler {

  type OutEither[F[_], A] = Outcome[EitherT[F, NonEmptyList[Error], *], Throwable, A]

  def embed[F[_] : Concurrent, A](out: OutEither[F, A]): EitherT[F, NonEmptyList[Error], A] =
    out.embed(EitherT.leftT(NonEmptyList.one(InterruptedExecution)))

  def join[F[_] : Concurrent, A1, A2]: ((OutEither[F, A1], OutEither[F, A2])) => EitherT[F, NonEmptyList[Error], (A1, A2)] = {

    case (out1, out2) =>
      embed(out1).biflatMap[NonEmptyList[Error], (A1, A2)](
        e1 => embed(out2).biflatMap(e2 => EitherT.leftT(e1 ::: e2), _ => EitherT.leftT(e1)),
        a1 => embed(out2).biflatMap(e2 => EitherT.leftT(e2), a2 => EitherT.rightT((a1, a2))),
      )

  }

  def parallel[F[_] : Concurrent, A1, A2, B](first: EitherT[F, NonEmptyList[Error], A1],
                                             second: EitherT[F, NonEmptyList[Error], A2],
                                             apply: (A1, A2) => B): EitherT[F, NonEmptyList[Error], B] =
    first.bothOutcome(second).flatMap(join).map(tuple => apply(tuple._1, tuple._2))

  def getFollowers[F[_] : Concurrent : Clock : Client : Logger, L](username: String,
                                                                   twitter: TwitterService[F])
                                                                  (implicit pl: ProgramLog[L],
                                                                   twConnection: TwitterConnection): EitherT[F, NonEmptyList[Error], Followers] =
    twitter.getUserByUsername(username)
      .flatMap(user => twitter.getUserFollowers(user.data.id))
      .leftMap({
        case terror.NotFound(_) => InvalidTwitterUser(username)
        case error => InternalTwitterError(username, error)
      })
      .leftMap(NonEmptyList.one)

  def getOrganizations[F[_] : Concurrent : Clock : Client : Logger, L](username: String,
                                                                       github: GitHubService[F])
                                                                      (implicit pl: ProgramLog[L],
                                                                       twConnection: GitHubConnection): EitherT[F, NonEmptyList[Error], List[Organization]] =
    github.getOrganizations(username)
      .leftMap({
        case gerror.NotFound(_) => InvalidGitHubUser(username)
        case error => InternalGitHubError(username, error)
      })
      .leftMap(NonEmptyList.one)

  def checkConnection[F[_] : Concurrent : Clock : Client : Logger, L](github: GitHubService[F],
                                                                      twitter: TwitterService[F],
                                                                      developers: Developers)
                                                                     (implicit pl: ProgramLog[L],
                                                                      ghConnection: GitHubConnection,
                                                                      twConnection: TwitterConnection): EitherT[F, Errors, Connection] = {

    import pl._

    val parallelOrganizations: EitherT[F, NonEmptyList[Error], List[Organization]] =
      parallel[F, List[Organization], List[Organization], List[Organization]](
        getOrganizations(developers.first, github),
        getOrganizations(developers.second, github),
        (orgs1, orgs2) => orgs1.filter(orgs2.contains)
      )

    val parallelUsers: EitherT[F, NonEmptyList[Error], List[User]] =
      parallel[F, Followers, Followers, List[User]](
        getFollowers(developers.first, twitter),
        getFollowers(developers.second, twitter),
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

  def default[F[_] : Concurrent : Clock : Client : Logger, L : ProgramLog](github: GitHubService[F],
                                                                           twitter: TwitterService[F])
                                                                          (implicit ghConnection: GitHubConnection,
                                                                           twConnection: TwitterConnection): DevelopersHandler[F] =
    (developers: Developers) => DevelopersHandler.checkConnection(github, twitter, developers)

}
