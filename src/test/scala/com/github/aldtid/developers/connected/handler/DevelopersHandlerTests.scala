package com.github.aldtid.developers.connected.handler

import com.github.aldtid.developers.connected.handler.DevelopersHandler._
import com.github.aldtid.developers.connected.logging.ProgramLog
import com.github.aldtid.developers.connected.logging.json.jsonProgramLog
import com.github.aldtid.developers.connected.model.Developers
import com.github.aldtid.developers.connected.model.responses._
import com.github.aldtid.developers.connected.service.github.GitHubService
import com.github.aldtid.developers.connected.service.github.connection.GitHubConnection
import com.github.aldtid.developers.connected.service.github.{error => gerror}
import com.github.aldtid.developers.connected.service.github.response.Organization
import com.github.aldtid.developers.connected.service.twitter.TwitterService
import com.github.aldtid.developers.connected.service.twitter.connection._
import com.github.aldtid.developers.connected.service.twitter.{error => terror}
import com.github.aldtid.developers.connected.service.twitter.response._
import com.github.aldtid.developers.connected.util.TempCache
import com.github.aldtid.developers.connected.util.TempCache.{CacheValue, createCache}

import cats.Applicative
import cats.data.{EitherT, NonEmptyList}
import cats.effect.{Clock, Concurrent, IO, Resource}
import cats.effect.unsafe.implicits.global
import org.http4s.{Response, Uri}
import org.http4s.client.Client
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._


class DevelopersHandlerTests extends AnyFlatSpec with Matchers {

  implicit val logger: Logger[IO] = Slf4jLogger.getLogger

  def clock[F[_] : Applicative](duration: FiniteDuration): Clock[F] = new Clock[F] {

    def applicative: Applicative[F] = Applicative[F]

    def monotonic: F[FiniteDuration] = Applicative[F].pure(duration)

    def realTime: F[FiniteDuration] = Applicative[F].pure(duration)

  }

  "parallel" should "evaluate two EitherT instances in parallel and apply passed function" in {

    val either1: EitherT[IO, NonEmptyList[Error], String] = EitherT.right(IO.sleep(1.second) *> IO("hello"))
    val either2: EitherT[IO, NonEmptyList[Error], String] = EitherT.right(IO.sleep(1.second) *> IO("world"))

    parallel[IO, String, String, String](either1, either2, (s1, s2) => s"$s1 $s2")
      .value.unsafeRunTimed(1500.millis) shouldBe Some(Right("hello world"))

  }

  it should "evaluate two EitherT instances in parallel and return the first error list" in {

    val either1: EitherT[IO, NonEmptyList[Error], String] = EitherT.left(IO.sleep(1.second) *> IO(NonEmptyList.one(MissingResource)))
    val either2: EitherT[IO, NonEmptyList[Error], String] = EitherT.right(IO.sleep(1.second) *> IO("world"))

    parallel[IO, String, String, String](either1, either2, (s1, s2) => s"$s1 $s2")
      .value.unsafeRunTimed(1500.millis) shouldBe Some(Left(NonEmptyList.one(MissingResource)))

  }

  it should "evaluate two EitherT instances in parallel and return the second error list" in {

    val either1: EitherT[IO, NonEmptyList[Error], String] = EitherT.right(IO.sleep(1.second) *> IO("hello"))
    val either2: EitherT[IO, NonEmptyList[Error], String] = EitherT.left(IO.sleep(1.second) *> IO(NonEmptyList.one(MissingResource)))

    parallel[IO, String, String, String](either1, either2, (s1, s2) => s"$s1 $s2")
      .value.unsafeRunTimed(1500.millis) shouldBe Some(Left(NonEmptyList.one(MissingResource)))

  }

  it should "evaluate two EitherT instances in parallel and return both error lists appended" in {

    val either1: EitherT[IO, NonEmptyList[Error], String] = EitherT.left(IO.sleep(1.second) *> IO(NonEmptyList.one(MissingResource)))
    val either2: EitherT[IO, NonEmptyList[Error], String] = EitherT.left(IO.sleep(1.second) *> IO(NonEmptyList.one(InterruptedExecution)))

    parallel[IO, String, String, String](either1, either2, (s1, s2) => s"$s1 $s2")
      .value.unsafeRunTimed(1500.millis) shouldBe Some(Left(NonEmptyList.of(MissingResource, InterruptedExecution)))

  }

  "getOrSetE" should "access the cache to get the value and evaluate the EitherT as the cache is empty" in {

    import jsonProgramLog._

    implicit val clk: Clock[IO] = clock(5.seconds)

    (
      for {
        cache  <- createCache[IO, String, Either[String, String]]
        temp    = TempCache.default(cache)
        result <- getOrSetE("dev", EitherT.right[String](IO("hello world")), 5.seconds, temp, log, log).value
        map    <- cache.get
      } yield (result, map)
    ).unsafeRunSync() shouldBe (Right("hello world"), Map("dev" -> CacheValue(Right("hello world"), 10.seconds)))

  }

  it should "access the cache to get the value and return it" in {

    import jsonProgramLog._

    implicit val clk: Clock[IO] = clock(5.seconds)

    (
      for {
        cache  <- createCache[IO, String, Either[String, String]]
        _      <- cache.set(Map("dev" -> CacheValue(Right("hello world"), 15.seconds)))
        temp    = TempCache.default(cache)
        result <- getOrSetE("dev", EitherT.left[String](IO("world hello")), 5.seconds, temp, log, log).value
        map    <- cache.get
      } yield (result, map)
      ).unsafeRunSync() shouldBe (Right("hello world"), Map("dev" -> CacheValue(Right("hello world"), 15.seconds)))

  }

  "getFollowers" should "get an user by its username and use it to return the user followers" in {

    implicit val connection: TwitterConnection = TwitterConnection(Uri(), "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev") EitherT.rightT(UserData(Some(User("id", "name", username)), None))
        else EitherT.leftT(terror.BadRequest("body"))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        if (id == "id") EitherT.rightT(Followers(Some(List(User(id, "name", "username"))), None, None))
        else EitherT.leftT(terror.BadRequest("body"))

    }

    getFollowers("dev", service).value.unsafeRunSync() shouldBe
      Right(UserFollowers(User("id", "name", "dev"), Followers(Some(List(User("id", "name", "username"))), None, None)))

  }

  it  should "map a BadRequest into an InvalidTwitterUser after retrieving an user" in {

    implicit val connection: TwitterConnection = TwitterConnection(Uri(), "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev") EitherT.leftT(terror.BadRequest("body"))
        else EitherT.rightT(UserData(Some(User("id", "name", username)), None))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        EitherT.rightT(Followers(Some(List(User(id, "name", "username"))), None, None))

    }

    getFollowers("dev", service).value.unsafeRunSync() shouldBe Left(InvalidTwitterUser("dev"))

  }

  it  should "map an Unauthorized into an InternalTwitterError after retrieving an user" in {

    implicit val connection: TwitterConnection = TwitterConnection(Uri(), "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev") EitherT.leftT(terror.Unauthorized("body"))
        else EitherT.rightT(UserData(Some(User("id", "name", username)), None))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        EitherT.rightT(Followers(Some(List(User(id, "name", "username"))), None, None))

    }

    getFollowers("dev", service).value.unsafeRunSync() shouldBe
      Left(InternalTwitterError("dev", terror.Unauthorized("body")))

  }

  it  should "map an UserData into an InvalidTwitterUser after retrieving an user if the data is None" in {

    implicit val connection: TwitterConnection = TwitterConnection(Uri(), "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev") EitherT.rightT(UserData(None, None))
        else EitherT.leftT(terror.Unauthorized("body"))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        EitherT.rightT(Followers(Some(List(User(id, "name", "username"))), None, None))

    }

    getFollowers("dev", service).value.unsafeRunSync() shouldBe Left(InvalidTwitterUser("dev"))

  }

  it  should "map an Unauthorized into an InternalTwitterError after retrieving the followers for an user" in {

    implicit val connection: TwitterConnection = TwitterConnection(Uri(), "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev") EitherT.rightT(UserData(Some(User("id", "name", username)), None))
        else EitherT.leftT(terror.BadRequest("body"))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        if (id == "id") EitherT.leftT(terror.Unauthorized("body"))
        else EitherT.rightT(Followers(Some(List(User(id, "name", "username"))), None, None))

    }

    getFollowers("dev", service).value.unsafeRunSync() shouldBe
      Left(InternalTwitterError("dev", terror.Unauthorized("body")))

  }

  "getOrganizations" should "get an user organizations" in {

    implicit val connection: GitHubConnection = GitHubConnection(Uri(), "username", "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: GitHubService[IO] = new GitHubService[IO] {

      def getOrganizations[L: ProgramLog](username: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: GitHubConnection): EitherT[IO, gerror.Error, List[Organization]] =
        if (username == "dev") EitherT.rightT(List(Organization("login", 123)))
        else EitherT.leftT(gerror.NotFound("body"))

    }

    getOrganizations("dev", service).value.unsafeRunSync() shouldBe Right(List(Organization("login", 123)))

  }

  it should "map a NotFound error to an InvalidGitHubUser after retrieving the organizations" in {

    implicit val connection: GitHubConnection = GitHubConnection(Uri(), "username", "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: GitHubService[IO] = new GitHubService[IO] {

      def getOrganizations[L: ProgramLog](username: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: GitHubConnection): EitherT[IO, gerror.Error, List[Organization]] =
        if (username == "dev") EitherT.leftT(gerror.NotFound("body"))
        else EitherT.rightT(List(Organization("login", 123)))

    }

    getOrganizations("dev", service).value.unsafeRunSync() shouldBe Left(InvalidGitHubUser("dev"))

  }

  it should "map an Unauthorized error to an InternalGitHubError after retrieving the organizations" in {

    implicit val connection: GitHubConnection = GitHubConnection(Uri(), "username", "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val service: GitHubService[IO] = new GitHubService[IO] {

      def getOrganizations[L: ProgramLog](username: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: GitHubConnection): EitherT[IO, gerror.Error, List[Organization]] =
        if (username == "dev") EitherT.leftT(gerror.Unauthorized("body"))
        else EitherT.rightT(List(Organization("login", 123)))

    }

    getOrganizations("dev", service).value.unsafeRunSync() shouldBe
      Left(InternalGitHubError("dev", gerror.Unauthorized("body")))

  }

  "checkConnection" should "correctly return the connection between two developers" in {

    implicit val ghConnection: GitHubConnection = GitHubConnection(Uri(), "username", "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val organization1: Organization = Organization("org-1", 123)
    val organization2: Organization = Organization("org-2", 234)
    val organization3: Organization = Organization("org-3", 345)

    val ghService: GitHubService[IO] = new GitHubService[IO] {

      def getOrganizations[L: ProgramLog](username: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: GitHubConnection): EitherT[IO, gerror.Error, List[Organization]] =
        if (username == "dev1") EitherT.rightT(List(organization1, organization2))
        else if (username == "dev2") EitherT.rightT(List(organization1, organization3))
        else EitherT.leftT(gerror.NotFound("body"))

    }

    implicit val twConnection: TwitterConnection = TwitterConnection(Uri(), "token")

    val user1: User = User("id1", "name1", "dev1")
    val user2: User = User("id2", "name2", "dev2")
    val followers1: Followers = Followers(Some(List(user2)), None, None)
    val followers2: Followers = Followers(Some(List(user1)), None, None)

    val twService: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev1") EitherT.rightT(UserData(Some(user1), None))
        else if (username == "dev2") EitherT.rightT(UserData(Some(user2), None))
        else EitherT.leftT(terror.BadRequest("body"))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        if (id == "id1") EitherT.rightT(followers1)
        else if (id == "id2") EitherT.rightT(followers2)
        else EitherT.leftT(terror.BadRequest("body"))

    }

    (
      for {

        orgsCache <- createCache[IO, String, Either[NonEmptyList[Error], List[Organization]]]
        orgsTemp   = TempCache.default(orgsCache)
        folCache  <- createCache[IO, String, Either[NonEmptyList[Error], UserFollowers]]
        folTemp    = TempCache.default(folCache)

        result    <- checkConnection(ghService, twService, orgsTemp, folTemp, 5.seconds, Developers("dev1", "dev2")).value
        orgsMap   <- orgsCache.get
        folMap    <- folCache.get

      } yield (result, orgsMap, folMap)
    ).unsafeRunSync() shouldBe
      (
        Right(Connected(NonEmptyList.one("org-1"))),
        Map(
          "dev1" -> CacheValue(Right(List(organization1, organization2)), 5.seconds),
          "dev2" -> CacheValue(Right(List(organization1, organization3)), 5.seconds)
        ),
        Map(
          "dev1" -> CacheValue(Right(UserFollowers(user1, followers1)), 5.seconds),
          "dev2" -> CacheValue(Right(UserFollowers(user2, followers2)), 5.seconds)
        )
      )

  }

  it should "return a NotConnected instance if users do not have common organizaitons" in {

    implicit val ghConnection: GitHubConnection = GitHubConnection(Uri(), "username", "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val organization1: Organization = Organization("org-1", 123)
    val organization2: Organization = Organization("org-2", 234)
    val organization3: Organization = Organization("org-3", 345)

    val ghService: GitHubService[IO] = new GitHubService[IO] {

      def getOrganizations[L: ProgramLog](username: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: GitHubConnection): EitherT[IO, gerror.Error, List[Organization]] =
        if (username == "dev1") EitherT.rightT(List(organization2))
        else if (username == "dev2") EitherT.rightT(List(organization3))
        else EitherT.leftT(gerror.NotFound("body"))

    }

    implicit val twConnection: TwitterConnection = TwitterConnection(Uri(), "token")

    val user1: User = User("id1", "name1", "dev1")
    val user2: User = User("id2", "name2", "dev2")
    val followers1: Followers = Followers(Some(List(user2)), None, None)
    val followers2: Followers = Followers(Some(List(user1)), None, None)

    val twService: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev1") EitherT.rightT(UserData(Some(user1), None))
        else if (username == "dev2") EitherT.rightT(UserData(Some(user2), None))
        else EitherT.leftT(terror.BadRequest("body"))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        if (id == "id1") EitherT.rightT(followers1)
        else if (id == "id2") EitherT.rightT(followers2)
        else EitherT.leftT(terror.BadRequest("body"))

    }

    (
      for {

        orgsCache <- createCache[IO, String, Either[NonEmptyList[Error], List[Organization]]]
        orgsTemp   = TempCache.default(orgsCache)
        folCache  <- createCache[IO, String, Either[NonEmptyList[Error], UserFollowers]]
        folTemp    = TempCache.default(folCache)

        result    <- checkConnection(ghService, twService, orgsTemp, folTemp, 5.seconds, Developers("dev1", "dev2")).value
        orgsMap   <- orgsCache.get
        folMap    <- folCache.get

      } yield (result, orgsMap, folMap)
    ).unsafeRunSync() shouldBe
    (
      Right(NotConnected),
      Map(
        "dev1" -> CacheValue(Right(List(organization2)), 5.seconds),
        "dev2" -> CacheValue(Right(List(organization3)), 5.seconds)
      ),
      Map(
        "dev1" -> CacheValue(Right(UserFollowers(user1, followers1)), 5.seconds),
        "dev2" -> CacheValue(Right(UserFollowers(user2, followers2)), 5.seconds)
      )
    )

  }

  it should "return a NotConnected instance if at least one of the users do not follow the other" in {

    implicit val ghConnection: GitHubConnection = GitHubConnection(Uri(), "username", "token")
    implicit val clk: Clock[IO] = clock(0.seconds)
    implicit val client: Client[IO] = Client(_ => Resource.pure(Response()))

    val organization1: Organization = Organization("org-1", 123)
    val organization2: Organization = Organization("org-2", 234)
    val organization3: Organization = Organization("org-3", 345)

    val ghService: GitHubService[IO] = new GitHubService[IO] {

      def getOrganizations[L: ProgramLog](username: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: GitHubConnection): EitherT[IO, gerror.Error, List[Organization]] =
        if (username == "dev1") EitherT.rightT(List(organization1, organization2))
        else if (username == "dev2") EitherT.rightT(List(organization1, organization3))
        else EitherT.leftT(gerror.NotFound("body"))

    }

    implicit val twConnection: TwitterConnection = TwitterConnection(Uri(), "token")

    val user1: User = User("id1", "name1", "dev1")
    val user2: User = User("id2", "name2", "dev2")
    val followers1: Followers = Followers(Some(List(user2)), None, None)
    val followers2: Followers = Followers(None, None, None)

    val twService: TwitterService[IO] = new TwitterService[IO] {

      def getUserByUsername[L: ProgramLog](username: String)
                                          (implicit F: Concurrent[IO],
                                           C: Clock[IO],
                                           client: Client[IO],
                                           logger: Logger[IO],
                                           connection: TwitterConnection): EitherT[IO, terror.Error, UserData] =
        if (username == "dev1") EitherT.rightT(UserData(Some(user1), None))
        else if (username == "dev2") EitherT.rightT(UserData(Some(user2), None))
        else EitherT.leftT(terror.BadRequest("body"))

      def getUserFollowers[L: ProgramLog](id: String)
                                         (implicit F: Concurrent[IO],
                                          C: Clock[IO],
                                          client: Client[IO],
                                          logger: Logger[IO],
                                          connection: TwitterConnection): EitherT[IO, terror.Error, Followers] =
        if (id == "id1") EitherT.rightT(followers1)
        else if (id == "id2") EitherT.rightT(followers2)
        else EitherT.leftT(terror.BadRequest("body"))

    }

    (
      for {

        orgsCache <- createCache[IO, String, Either[NonEmptyList[Error], List[Organization]]]
        orgsTemp   = TempCache.default(orgsCache)
        folCache  <- createCache[IO, String, Either[NonEmptyList[Error], UserFollowers]]
        folTemp    = TempCache.default(folCache)

        result    <- checkConnection(ghService, twService, orgsTemp, folTemp, 5.seconds, Developers("dev1", "dev2")).value
        orgsMap   <- orgsCache.get
        folMap    <- folCache.get

      } yield (result, orgsMap, folMap)
    ).unsafeRunSync() shouldBe
    (
      Right(NotConnected),
      Map(
        "dev1" -> CacheValue(Right(List(organization1, organization2)), 5.seconds),
        "dev2" -> CacheValue(Right(List(organization1, organization3)), 5.seconds)
      ),
      Map(
        "dev1" -> CacheValue(Right(UserFollowers(user1, followers1)), 5.seconds),
        "dev2" -> CacheValue(Right(UserFollowers(user2, followers2)), 5.seconds)
      )
    )

  }

}
