package net.walend.duck.front

import fs2.Stream
import cats.implicits.*
import cats.effect.{FiberIO, IO, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdateService, GeoPoint}
import org.scalajs.dom.HTMLDocument
import smithy4s.http.UnknownErrorResponse

import scala.concurrent.duration.DurationInt

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
case class DuckMapUpdater(
                           client: DuckUpdateService[IO], 
                           eventStore: EventStore[IO], 
                           document: HTMLDocument,
                           geoIO: GeoIO , 
                           duckId: DuckId
                         ):

  private def startUpdates(): Resource[IO, Unit] =
    for
      _ <- startMap()
      _ <- startPinger()
    yield ()

  def updateForever(): IO[Nothing] = startUpdates().useForever

  private def startMap(): Resource[IO, FiberIO[Unit]] =
    def mapUpdateStream(duckView: MapLibreDuckView) =
      Stream.fixedRateStartImmediately[IO](1.seconds, dampen = true)
        .evalMap(_ => redrawMap(duckView))
        .compile.drain.start
        .toResource

    for
      mapLibre <- MapLibreGL.mapLibreResource(geoIO, client)
      duckView <- MapLibreDuckView.create(mapLibre, document)
      fiber <- mapUpdateStream(duckView)
    yield
      fiber

  private def redrawMap(
                         duckView: MapLibreDuckView,
                       ): IO[Unit] =
    val p: IO[Unit] = for
      eventsFromServer <- eventStore.allEvents
      sitRep = SitRep(eventsFromServer)
      now <- IO.realTime
      _ <- duckView.updateMapLibre(sitRep, now)
    yield ()
    p.recover {
      case uer: UnknownErrorResponse if uer.code == 504 =>
        println(s"${uer.getMessage}. Will try redrawMap again.")
      case x =>
        x.printStackTrace() //todo remove when you haven't trapped a problem in a while
    }
    
  private def startPinger(): Resource[IO, FiberIO[Unit]] =
    Stream.fixedRateStartImmediately[IO](10.seconds, dampen = true) //todo change to every 30 seconds -  or even variable control with some feedback
      .evalMap(_ => ping())
      .compile.drain.start
      .toResource

  private def ping(): IO[Unit] =
    val p: IO[Unit] = for
      position: GeoPoint <- geoIO.position()
      _ <- IO.println(s"Ping from ${position.latitude},${position.longitude}!")
      eventsFromServer <- eventStore.sendPositionAndGetUpdates(position, client, duckId)
      sitRep = SitRep(eventsFromServer)
      _ <- IO.println(sitRep)
    yield ()
    p.recover {
      case uer: UnknownErrorResponse if uer.code == 504 =>
        println(s"${uer.getMessage}. Will try ping again.")
    }

    