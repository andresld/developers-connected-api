package com.github.aldtid.developers.connected.logging

trait LogOps {

  implicit def formattedLog[L](log: Log[L]): String = log.formatted

  implicit def formattedLoggable[A, L](a: A)(implicit log: Log[L], loggable: Loggable[A, L]): String =
    loggableAsLog(a).formatted

  implicit def loggableAsLog[A, L](a: A)(implicit log: Log[L], loggable: Loggable[A, L]): Log[L] = log |+| a

}
