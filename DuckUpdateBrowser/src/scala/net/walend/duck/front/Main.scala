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
import typings.maplibreGl.global.maplibregl.Map as MapLibreMap

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

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
      _ <- eventStore.sendDuckInfo(duckName,client).toResource
      appDiv <- div("") //todo eventually make this a control overlay
      duckId <- client.getDuckId(duckName).map(_.duckId).toResource //todo remember the user if possible
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
    MapLibreGL.mapLibreResource(geoIO, client).use { mapLibre =>
      Stream.fixedRateStartImmediately[IO](10.seconds,dampen = true) //todo change to every 30 seconds -  or even variable control with some feedback
        .evalMap(_ => ping(geoIO, client, eventStore, mapLibre, duckId))
        .compile.drain.start
    }.toResource

  private def ping(
                    geoIO: GeoIO,
                    client: DuckUpdateService[IO],
                    eventStore: EventStore[IO],
                    mapLibre: MapLibreMap,
                    duckId: DuckId,
                  ): IO[Unit]  =
    val p:IO[Unit] = for
      position: GeoPoint <- geoIO.position()
      _ <- IO.println(s"Ping from ${position.latitude},${position.longitude}!")
      eventsFromServer <- eventStore.sendPositionAndGetUpdates(position,client,duckId)
      sitRep = SitRep(eventsFromServer)
      _ <- IO.println(sitRep)
      _ <- MapLibreGL.updateMapLibre[IO](mapLibre,sitRep)
    yield ()
    p.recover {
      case uer: UnknownErrorResponse if uer.code == 504 =>
        println(s"${uer.getMessage}. Will try again.")
    }
