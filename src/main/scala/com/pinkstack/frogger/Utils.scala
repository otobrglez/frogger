package com.pinkstack.frogger
import zio.{Console, ZIO}
import ZIO.succeed
import Console.printLine

import java.time.Instant
import java.time.temporal.ChronoUnit

object Utils:
  def timeIt[E, A](tag: String)(zio: ZIO[Any, E, A]): ZIO[Any, E, A] =
    for
      startedAt <- printLine(s"[$tag] 🐇").orDie *> succeed(Instant.now)
      out       <- zio <* printLine(s"[$tag] 🏁 ${ChronoUnit.MILLIS.between(startedAt, Instant.now)} ms.").orDie
    yield out
