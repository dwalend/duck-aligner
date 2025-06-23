package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{FiberIO, IO, Resource}
import fs2.dom.HtmlElement
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}
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
      document = window.document.asInstanceOf[org.scalajs.dom.html.Document] //todo should not need to cast
      geoIO = GeoIO(document)
      duckName = duckNameFromUriQuery(document)
      appDiv <- div("") //todo eventually make this a control overlay
      duckId <- client.getDuckId(duckName).map(_.duckId).toResource //todo remember the user if possible
      _ <- startPinger(geoIO,client,duckId,duckName)
    yield
      println("See ducks!")
      appDiv

  private def duckNameFromUriQuery(document:org.scalajs.dom.html.Document):String =
    val uri = Uri.unsafeFromString(document.documentURI)
    uri.query.pairs.toMap.apply("duckName").get

  private def startPinger(geoIO: GeoIO,client: DuckUpdateService[IO],duckId: DuckId,duckName: String): Resource[IO, FiberIO[Unit]] =
    MapLibreGL.mapLibreResource(geoIO, client).use { mapLibre =>
      Stream.fixedRateStartImmediately[IO](10.seconds,dampen = true) //todo change to every 30 seconds -  or even variable control with some feedback
        .evalMap(_ => ping(geoIO, client, mapLibre, duckId, duckName))
        .compile.drain.start
    }.toResource

  private def ping(geoIO: GeoIO,client: DuckUpdateService[IO],mapLibre: MapLibreMap,duckId: DuckId,duckName: String): IO[Unit]  =
    val p = for
      position: GeoPoint <- geoIO.position()
      _ <- IO.println(s"Ping from ${position.latitude},${position.longitude}!")
      update: UpdatePositionOutput <- updatePosition[IO](position,client,duckId,duckName)
      _ <- IO.println(update.sitRep)
      _ <- MapLibreGL.updateMapLibre[IO](mapLibre,update)
    yield
      ()
    p.recover {
      case uer: UnknownErrorResponse if uer.code == 504 =>
        println(s"${uer.getMessage}. Will try again.")
    }

  private def updatePosition[F[_]](position: GeoPoint,client: DuckUpdateService[F],duckId: DuckId,duckName: String): F[UpdatePositionOutput] =
    val duckUpdate: DuckUpdate = DuckUpdate(
      id = duckId,
      duckName = duckName,  //todo update a counter maybe - maybe not needed
      position = position
    )
    client.updatePosition(duckUpdate)


