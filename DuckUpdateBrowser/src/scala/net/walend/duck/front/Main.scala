package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import org.scalajs.dom.{HTMLImageElement, Position, document}
import fs2.dom.{HtmlDocument, HtmlElement}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}
import typings.geojson.mod.{Feature, FeatureCollection, Point}

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import typings.maplibreGl.global.maplibregl.Map as MapLibreMap
import typings.maplibreGl.mod.{GetResourceResponse, MapOptions}
import typings.maplibreMaplibreGlStyleSpec.anon.Iconallowoverlap
import typings.maplibreMaplibreGlStyleSpec.mod.{LayerSpecification, SourceSpecification}
import typings.std.ImageBitmap

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
      apiKey: String <- client.mapLibreGlKey().map(_.key).toResource

      doc: HtmlDocument[IO] = window.document
      geoIO = GeoIO(document)
      position: Position <- geoIO.positionResource()
      update: UpdatePositionOutput <- updatePosition(position,client)
      _ <- IO.println(s"Hello ${position.coords.latitude},${position.coords.longitude}!").toResource
      mapLibre <- mapLibreResource(apiKey,update)
      appDiv <- div("I wish this were the map")
      _ <- updateMapLibre(mapLibre,update)
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

  private def mapLibreResource(apiKey:String, update: UpdatePositionOutput): Resource[IO, MapLibreMap] =
    val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
    val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.

    val p: GeoPoint = update.sitRep.tracks.head.positions.head

    val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"
    IO.blocking{new MapLibreMap(new MapOptions {
      style = styleUrl
      var container = "map"
      center = (p.longitude,p.latitude)
      zoom = 14
    })}.toResource

  private def updateMapLibre(mapLibre:MapLibreMap,update: UpdatePositionOutput) =
    val loadImage = IO.fromFuture(IO.blocking(mapLibre.loadImage("https://upload.wikimedia.org/wikipedia/commons/7/7c/201408_cat.png")
      .toFuture))
    val addImage: IO[mapLibre.type] = loadImage.map((i: GetResourceResponse[HTMLImageElement | ImageBitmap]) => i.data match {
      case element: HTMLImageElement => mapLibre.addImage("cat",element)
      case bitmap => mapLibre.addImage("cat",bitmap.asInstanceOf[typings.std.global.ImageBitmap])
    })

    val p: GeoPoint = update.sitRep.tracks.head.positions.head

    val featureSpec = SourceSpecification.GeoJSONSourceSpecification(FeatureCollection(
      js.Array(Feature(Point(js.Array(p.longitude,p.latitude)),""))
    ))
    addImage.map { _ =>
      val layerSpec = LayerSpecification.SymbolLayerSpecification("points", "point").setLayout(Iconallowoverlap().`setIcon-image`("cat").`setIcon-size`(0.25))
      mapLibre.addSource("point", featureSpec)
      mapLibre.addLayer(layerSpec)
    }.toResource