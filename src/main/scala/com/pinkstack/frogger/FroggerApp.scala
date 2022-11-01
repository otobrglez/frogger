package com.pinkstack.frogger
import com.pinkstack.frogger.BuildInfo as BI
import zio.*
import ZIO.{fail, fromTry, succeed}
import Console.printLine
import zio.stream.{Stream, ZPipeline, ZSink, ZStream}
import zio.stm._

import scala.reflect.{classTag, ClassTag}
import java.nio.file.{Path, Paths}
import scala.util.Try
import Models.*
import Utils.timeIt

import java.time.Instant
import java.time.temporal.ChronoUnit

object Queries:
  val trips: Stream[Throwable, Trip]         = Trip.stream.take(10_0000)
  val stopTimes: Stream[Throwable, StopTime] = StopTime.stream.take(10_0000)
  val routes: Stream[Throwable, Route]       = Route.stream.take(10_0000)

  // TODO: Why not fold into TRef for Map.
  private def foldStreamByKey[A, K](stream: ZStream[Any, Throwable, A])(f: A => K): Task[Map[K, Chunk[A]]] =
    stream
      .groupByKey(f) { case (k, stream) =>
        ZStream.fromZIO(stream.runCollect.map(chunk => k -> chunk))
      }
      .runFold(Map.empty[K, Chunk[A]].withDefaultValue(Chunk.empty[A])) { case (agg, (k, chunk)) =>
        agg + (k -> (agg.get(k) match {
          case Some(v) => v ++ chunk
          case _       => chunk
        }))
      }

  val tripsByRoute: Task[Map[RouteID, Chunk[Trip]]]       = foldStreamByKey(trips)(_.routeID)
  val stopTimesByTrip: Task[Map[TripID, Chunk[StopTime]]] = foldStreamByKey(stopTimes)(_.tripID)
  val allRoutes: Task[Map[RouteID, Route]] = foldStreamByKey(routes)(_.routeID).map(_.view.mapValues(_.head).toMap)

object FroggerApp extends zio.ZIOAppDefault:
  def program =
    for
      tripsR  <- TMap.empty[RouteID, Chunk[Trip]].commit
      stopsR  <- TMap.empty[TripID, Chunk[StopTime]].commit
      routesR <- TMap.empty[RouteID, Route].commit

      _ <- printLine(s"Booting ${BI.toString}")

      maps <-
        timeIt("Loading data")(
          timeIt("Trips per route aggregation")(Queries.tripsByRoute)
            <&> timeIt("Stop times per trip aggregation")(Queries.stopTimesByTrip)
            <&> timeIt("Loading routes")(Queries.allRoutes)
        )

      _ <- succeed(maps._1).flatMap { p =>
        succeed(println("ok"))
      }
      // red <- tripsR.size.commit
      // _   <- printLine(s"Routes: ${red}")
      _ <- printLine("Booting server...")
    yield ()

  def run = program
