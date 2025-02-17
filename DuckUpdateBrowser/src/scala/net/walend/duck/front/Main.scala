package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import org.scalajs.dom.{Position, document}
import fs2.dom.{HtmlDocument, HtmlElement}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import typings.maplibreGl.global.maplibregl.Map
import typings.maplibreGl.mod.MapOptions

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
      apiKey: String <- client.mapLibreGlKey().map(_.key).toResource

      doc: HtmlDocument[IO] = window.document
      //      geoLocator: GeoLocator = GeoLocator.geolocator(document,client)
      geoIO = GeoIO(document)
      position: Position <- geoIO.positionResource()
      update: UpdatePositionOutput <- updatePosition(position,client)
      _ <- IO.println(s"Hello ${position.coords.latitude},${position.coords.longitude}!").toResource
      appDiv <- div("I wish this were the map")
      mapLibre <- mapLibreResource(apiKey,update)
    yield
      println("DuckUpdateClient ducks!")
      appDiv

  private def updatePosition(position: Position,client: DuckUpdateService[IO]): Resource[IO, UpdatePositionOutput] =
    val geoPoint: GeoPoint = GeoPoint(
      latitude = position.coords.latitude,
      longitude = position.coords.longitude,
      timestamp = position.timestamp.toLong
    )
    val duckUpdate: DuckUpdate = DuckUpdate(
      id = DuckId(0),
      snapshot = 0,
      position = geoPoint
    )
    client.updatePosition(duckUpdate).toResource  
  
  
  private def mapLibreResource(apiKey:String, update: UpdatePositionOutput): Resource[IO, Map] =
    val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
    val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.

    val p: GeoPoint = update.tracks.tracks.head.positions.head
    
    val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"
    IO{new Map(new MapOptions {
      style = styleUrl
      var container = "map"
      center = (p.longitude,p.latitude)
      zoom = 14
    })}.toResource


/*


    println("console")
    label("Hello!")

    for
//      client <- DuckUpdateClient.duckUpdateClient
      hi <- label("Hello!")
      _ = println("console")
//      geoLocator = GeoLocator.geolocator(document,client)
//      mapDiv <- div("map")//MapLibreGL.mapDiv(client)
    yield
      println("DuckUpdateClient ducks!")
      hi
*/
//      geoLocator.geoLocate()
      /*
      val map = new Map(new MapOptions {
        style = styleUrl
        var container = "map1"
        center = (25.24, 36.31)
        zoom = 2
      })
      */
