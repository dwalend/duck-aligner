package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{FiberIO, IO, Resource}
import fs2.dom.HtmlElement
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdateService, GeoPoint}
import fs2.Stream
import cats.implicits.*
import org.http4s.Uri
import smithy4s.http.UnknownErrorResponse

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.concurrent.duration.DurationInt

@JSExportTopLevel("Main")
object Main extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)
  
  def render: Resource[IO, HtmlElement[IO]] =
    import calico.html.io.{*, given}

    for
      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      eventStore <- EventStore.create[IO]()
      document = window.document.asInstanceOf[org.scalajs.dom.html.Document] //todo should not need to cast
      geoIO = GeoIO(document)
      duckName = duckNameFromUriQuery(document) //todo send via proposing an event
      duckId <- eventStore.sendDuckInfo(duckName,client).toResource
      appDiv <- div("") //todo eventually make this a control overlay
      _ <- startMap(geoIO,client,eventStore,document)
      _ <- startPinger(geoIO,client,eventStore,duckId)
    yield
      println("See ducks!")
      appDiv

  private def duckNameFromUriQuery(document:org.scalajs.dom.html.Document):String =
    val uri = Uri.unsafeFromString(document.documentURI)
    uri.query.pairs.toMap.apply("duckName").get

  private def startPinger(
                           geoIO: GeoIO,
                           client: DuckUpdateService[IO],
                           eventStore: EventStore[IO],
                           duckId: DuckId,
                         ): Resource[IO, FiberIO[Unit]] =
    Stream.fixedRateStartImmediately[IO](10.seconds,dampen = true) //todo change to every 30 seconds -  or even variable control with some feedback
      .evalMap(_ => ping(geoIO, client, eventStore, duckId))
      .compile.drain.start
      .toResource

  private def ping(
                    geoIO: GeoIO,
                    client: DuckUpdateService[IO],
                    eventStore: EventStore[IO],
                    duckId: DuckId,
                  ): IO[Unit]  =
    val p:IO[Unit] = for
      position: GeoPoint <- geoIO.position()
      _ <- IO.println(s"Ping from ${position.latitude},${position.longitude}!")
      eventsFromServer <- eventStore.sendPositionAndGetUpdates(position,client,duckId)
      sitRep = SitRep(eventsFromServer)
      _ <- IO.println(sitRep)
    yield ()
    p.recover {
      case uer: UnknownErrorResponse if uer.code == 504 =>
        println(s"${uer.getMessage}. Will try ping again.")
    }

  private def startMap(
                        geoIO: GeoIO,
                        client: DuckUpdateService[IO],
                        eventStore: EventStore[IO],
                        document: org.scalajs.dom.html.Document,
                      ): Resource[IO, FiberIO[Unit]] =
    def mapUpdateStream(duckView: MapLibreDuckView) =
      Stream.fixedRateStartImmediately[IO](1.seconds,dampen = true)
        .evalMap(_ => redrawMap(eventStore, duckView))
        .compile.drain.start
        .toResource

    for
      mapLibre <- MapLibreGL.mapLibreResource(geoIO, client)
      duckView <- MapLibreDuckView.create(mapLibre, document)
      fiber <- mapUpdateStream(duckView)
    yield
      fiber

  private def redrawMap(
                          eventStore: EventStore[IO],
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
