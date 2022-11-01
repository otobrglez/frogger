package com.pinkstack.frogger
import com.pinkstack.frogger.Loader.Index
import zio.*
import zio.ZIO.{fromTry, succeed}
import zio.stream.{Stream, ZStream}

import java.nio.file.Paths
import java.time.LocalTime
import java.time.format.{DateTimeFormatter, ResolverStyle}
import scala.util.Try

object Models:
  type TripID    = String
  type RouteID   = String
  type ServiceID = String
  type AgencyID  = String

  private def tryDecode[T](row: (Array[String], Index))(f: => ((Array[String], Index)) => T): ZIO[Any, Throwable, T] =
    fromTry(Try(f(row))).mapError(th => new RuntimeException(th.getMessage + s" for line ${row._2}"))

  case class StopTime(tripID: TripID, arrivalTime: LocalTime, departureTime: LocalTime, stopID: String)

  object StopTime:
    given Loader.Decoder[StopTime] with
      private val timeFormat: DateTimeFormatter =
        DateTimeFormatter.ISO_LOCAL_TIME.withResolverStyle(ResolverStyle.LENIENT)

      def decode(row: (Array[String], Index)): ZIO[Any, Throwable, StopTime] =
        tryDecode(row) { row =>
          val Array(tripID, arrivalTime, departureTime, stopID, _*) = row._1
          StopTime.apply(
            tripID,
            LocalTime.parse(arrivalTime, timeFormat),
            LocalTime.parse(departureTime, timeFormat),
            stopID
          )
        }

    def stream: Stream[Throwable, StopTime] =
      Loader.streamAs(Paths.get("./MBTA_GTFS/stop_times.txt"))

  case class Trip(routeID: RouteID, serviceID: ServiceID, tripID: TripID)

  object Trip:
    given Loader.Decoder[Trip] with
      def decode(row: (Array[String], Index)): ZIO[Any, Throwable, Trip] =
        tryDecode(row) { row =>
          val Array(routeID, serviceID, tripID, _*) = row._1
          Trip.apply(routeID, serviceID, tripID)
        }

    def stream: Stream[Throwable, Trip] =
      Loader.streamAs(Paths.get("./MBTA_GTFS/trips.txt"))

  case class Route(routeID: RouteID, agencyID: AgencyID, routeShortName: String, routeLongName: String)

  object Route:
    given Loader.Decoder[Route] with
      def decode(row: (Array[String], Index)): ZIO[Any, Throwable, Route] =
        tryDecode(row) { row =>
          val Array(routeID, agencyID, routeShortName, routeLongName, _*) = row._1
          Route.apply(routeID, agencyID, routeShortName, routeLongName)
        }

    def stream: Stream[Throwable, Route] =
      Loader.streamAs[Route](Paths.get("./MBTA_GTFS/routes.txt"))
