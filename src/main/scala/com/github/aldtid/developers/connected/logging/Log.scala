package com.github.aldtid.developers.connected.logging

import cats.Semigroup

trait Log[L] {

  val value: L

  def |+|[A](a: A)(implicit loggable: Loggable[A, L]): Log[L]

  def formatted: String

}

object Log {

  def createLog[L : Semigroup](_value: L, format: L => String): Log[L] = new Log[L] {

    val value: L = _value

    def |+|[A](a: A)(implicit loggable: Loggable[A, L]): Log[L] =
      createLog(Semigroup[L].combine(value, loggable.format(a)), format)

    def formatted: String = format(value)

  }

}
