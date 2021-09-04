package com.github.aldtid.developers.connected.logging

trait Loggable[-A, L] {

  def format(a: A): L

}
