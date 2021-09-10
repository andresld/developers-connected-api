package com.github.aldtid.developers.connected.util

import cats.Monad
import cats.effect.Ref
import cats.effect.kernel.Clock
import cats.implicits._

import scala.concurrent.duration._


/**
 * Represents a temporal cache. This cache has a mechanism to set a value ([[TempCache.set]]) that, along with key and
 * value, requires a timeout argument. This value will be used in later retrieves ([[TempCache.get]]) to check if the
 * value has expired or not. In case the value has expired, then that call must not return the saved value.
 *
 * @tparam F context type
 * @tparam K cache key type
 * @tparam V cache value type
 */
trait TempCache[F[_], K, V] {

  /**
   * Retrieves an element from the cache by its key.
   *
   * In case the key does not exist in the cache or the value has expired, a None will be returned.
   *
   * @param key key to look a value for
   * @return a None if the value does not exist or the value
   */
  def get(key: K): F[Option[V]]

  /**
   * Sets a pair key-value in the cache.
   *
   * The value expiration date is calculated with passed timeout.
   *
   * @param key key to index the value by
   * @param value related value to save and expire
   * @param timeout expiration timeout
   * @return a unit
   */
  def set(key: K, value: V, timeout: FiniteDuration): F[Unit]

}

object TempCache {

  // Auxiliary type to make the code clearer
  type Cache[F[_], K, V] = Ref[F, Map[K, CacheValue[V]]]

  final case class CacheValue[V](value: V, until: FiniteDuration)

  /**
   * Gets current date and saves the key and value in the cache, along with maximum life time.
   *
   * Life time is calculated by adding the timeout to current date, which results in a new date that marks the maximum
   * date for which this key value should be returned.
   *
   * @param key key to index the value by
   * @param value related value to save and expire
   * @param timeout expiration timeout
   * @param cache reference containing the cache
   * @tparam F context type
   * @tparam K cache key type
   * @tparam V cache value type
   * @return a unit
   */
  def set[F[_] : Clock : Monad, K, V](key: K, value: V, timeout: FiniteDuration, cache: Cache[F, K, V]): F[Unit] =
    for {
      current <- Clock[F].monotonic
      _       <- cache.update(_ + (key -> CacheValue(value, current + timeout)))
    } yield ()

  /**
   * Gets current date and cache value, if any. In case there is a value, its expiration time is checked and, if it is
   * a past date, then the value has expired and must not be returned.
   *
   * Once a value is expired, its entry in the cache is deleted in order to be more efficient for next gets.
   *
   * @param key key to look a value for
   * @param cache reference containing the cache
   * @tparam F context type
   * @tparam K cache key type
   * @tparam V cache value type
   * @return a None if the value does not exist or the value
   */
  def get[F[_] : Clock : Monad, K, V](key: K, cache: Cache[F, K, V]): F[Option[V]] =
    for {
      current <- Clock[F].monotonic
      value   <- cache.get.map(_.get(key))
      result  <- value.fold[F[Option[V]]](Monad[F].pure(None))(updateOrReturn(key, _, current, cache))
    } yield result

  /**
   * Checks if value date is lower than current date. In case it is, the entry is removed from the cache and a None is
   * returned; otherwise the cache value is returned.
   *
   * @param key key to look a value for
   * @param value saved cache value
   * @param current current date associated with get call
   * @param cache reference containing the cache
   * @tparam F context type
   * @tparam K cache key type
   * @tparam V cache value type
   * @return a None if the value does not exist or the value
   */
  def updateOrReturn[F[_] : Monad, K, V](key: K,
                                         value: CacheValue[V],
                                         current: FiniteDuration,
                                         cache: Cache[F, K, V]): F[Option[V]] =
      if (current > value.until) cache.update(_ - key).map(_ => None) else Monad[F].pure(Some(value.value))

  /**
   * Default implementation for a temporal cache, based on a reference containing a Map.
   *
   * @param cache reference containing the cache
   * @tparam F context type
   * @tparam K cache key type
   * @tparam V cache value type
   * @return the default temporal cache
   */
  def default[F[_] : Monad : Clock, K, V](cache: Cache[F, K, V]): TempCache[F, K, V] = new TempCache[F, K, V] {

    def get(key: K): F[Option[V]] = TempCache.get(key, cache)

    def set(key: K, value: V, timeout: FiniteDuration): F[Unit] = TempCache.set(key, value, timeout, cache)

  }

}
