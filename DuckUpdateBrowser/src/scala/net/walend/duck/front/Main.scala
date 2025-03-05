package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{FiberIO, IO, Resource}
import fs2.dom.HtmlElement
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}
import fs2.Stream
import cats.implicits.*

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
      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient
      document = window.document.asInstanceOf[org.scalajs.dom.html.Document] //todo should not need to cast
      geoIO = GeoIO(document)
      appDiv <- div("") //todo eventually make this a control overlay
      _ <- startPinger(geoIO,client)
    yield
      println("See ducks!")
      appDiv

  private def startPinger(geoIO: GeoIO,client: DuckUpdateService[IO]): Resource[IO, FiberIO[Unit]] =
    MapLibreGL.mapLibreResource(geoIO, client).use { mapLibre =>
      Stream.repeatEval(ping(geoIO, client, mapLibre))
        .meteredStartImmediately(10.seconds).debounce(5.seconds) //todo change to every 30 seconds
        .compile.drain.start
    }.toResource

  private def ping(geoIO: GeoIO,client: DuckUpdateService[IO],mapLibre: MapLibreMap): IO[UpdatePositionOutput]  =
    for
      position: GeoPoint <- geoIO.position()
      _ <- IO.println(s"Ping from ${position.latitude},${position.longitude}!")
      update: UpdatePositionOutput <- updatePosition(position,client)
      _ <- IO.println(update.sitRep)
      _ <- MapLibreGL.updateMapLibre(mapLibre,update)
    yield
      update

  private def updatePosition(position: GeoPoint,client: DuckUpdateService[IO]): IO[UpdatePositionOutput] =
    val duckUpdate: DuckUpdate = DuckUpdate(
      id = DuckId(0),  //todo get from start property
      snapshot = 0,  //todo update a counter
      position = position
    )
    client.updatePosition(duckUpdate)


