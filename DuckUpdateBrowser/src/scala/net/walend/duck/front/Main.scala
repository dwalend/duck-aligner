package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{FiberIO, IO, Resource}
import org.scalajs.dom.{HTMLImageElement, Position}
import fs2.dom.HtmlElement
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, Track, UpdatePositionOutput}
import typings.geojson.mod.{Feature, FeatureCollection, GeoJSON, GeoJsonProperties, Geometry, Point}
import fs2.Stream
import cats.implicits.*
import org.scalablytyped.runtime.StringDictionary

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import typings.maplibreGl.global.maplibregl.{GeoJSONSource, Map as MapLibreMap}
import typings.maplibreGl.mod.{GetResourceResponse, MapOptions}
import typings.maplibreMaplibreGlStyleSpec.anon.Iconallowoverlap
import typings.maplibreMaplibreGlStyleSpec.mod.{GeoJSONSourceSpecification, LayerSpecification, SourceSpecification}
import typings.std.ImageBitmap

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
      apiKey: String <- client.mapLibreGlKey().map(_.key).toResource

      document = window.document.asInstanceOf[org.scalajs.dom.html.Document] //todo should not need to cast
      geoIO = GeoIO(document)
      position: Position <- geoIO.positionResource()
      appDiv <- div("") //todo eventually make this a control overlay
      mapLibre: MapLibreMap <- mapLibreResource(apiKey,position.toGeoPoint)
      _ <- startPinger(geoIO,client,mapLibre)
    yield
      println("See ducks!")
      appDiv

  private def startPinger(geoIO: GeoIO,client: DuckUpdateService[IO],mapLibre: MapLibreMap): Resource[IO, FiberIO[Unit]] =
  //todo change to every 30 seconds
    Stream.repeatEval(ping(geoIO,client,mapLibre)).meteredStartImmediately(10.seconds).debounce(5.seconds)
      .compile.drain.start.toResource

  private def ping(geoIO: GeoIO,client: DuckUpdateService[IO],mapLibre: MapLibreMap): IO[UpdatePositionOutput]  =
    for
      position: Position <- geoIO.position()
      _ <- IO.println(s"Ping from ${position.coords.latitude},${position.coords.longitude}!")
      update: UpdatePositionOutput <- updatePosition(position,client)
      _ <- IO.println(update.sitRep)
      _ <- updateMapLibre(mapLibre,update)
    yield
      update

  private def updatePosition(position: Position,client: DuckUpdateService[IO]): IO[UpdatePositionOutput] =
    val duckUpdate: DuckUpdate = DuckUpdate(
      id = DuckId(0),  //todo get from start property
      snapshot = 0,  //todo update a counter
      position = position.toGeoPoint
    )
    client.updatePosition(duckUpdate)

  //todo move out the mapLibre pieces
  private def mapLibreResource(apiKey:String, c:GeoPoint): Resource[IO, MapLibreMap] =
    val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
    val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.

    val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"
    IO.blocking{new MapLibreMap(new MapOptions {
      style = styleUrl
      var container = "map"
      center = (c.longitude,c.latitude)
      zoom = 7 //7 is about a 3-hour drive from the center
    })}.toResource

  private def updateMapLibre(mapLibre:MapLibreMap,update: UpdatePositionOutput) =
    //todo add enough data to UpdatePositionOutput to figure out the image
    val duckTracks: Seq[Track] = update.sitRep.tracks

    //filter the list of ducks for ducks that don't have images
    val imageNames: js.Array[String] = mapLibre.listImages()
    val newDucks = duckTracks.filterNot(d => imageNames.contains(d.id.imageName))
    val addNewDucks = newDucks.map{ d =>
      //add an image for each of those ducks
      val imageName = d.id.imageName
      val loadImage = IO.fromFuture(IO.blocking(mapLibre.loadImage("https://upload.wikimedia.org/wikipedia/commons/7/7c/201408_cat.png")
        .toFuture))
      val addImage: IO[mapLibre.type] = loadImage.map((i: GetResourceResponse[HTMLImageElement | ImageBitmap]) => i.data match {
        case element: HTMLImageElement => mapLibre.addImage(imageName, element)
        case bitmap => mapLibre.addImage(imageName, bitmap.asInstanceOf[typings.std.global.ImageBitmap])
      })

      val p: GeoPoint = d.positions.head
      val featureSpec: GeoJSONSourceSpecification = SourceSpecification.GeoJSONSourceSpecification(FeatureCollection(
        js.Array(Feature(Point(js.Array(p.longitude, p.latitude)), ""))
      ))
      val sourceName = d.id.sourceName
      val layerName = d.id.layerName

      addImage.map { _ =>
        mapLibre.addSource(sourceName, featureSpec)
        val layerSpec = LayerSpecification.SymbolLayerSpecification(layerName, sourceName).setLayout(Iconallowoverlap().`setIcon-image`(imageName).`setIcon-size`(0.125))
        mapLibre.addLayer(layerSpec)
      }
    }.sequence
    //for each duck get the layer spec, move the feature spec
    val positionDucks = addNewDucks.map { _ =>
      duckTracks.map{ track =>
        val p: GeoPoint = track.positions.head
        val data:GeoJSON[Geometry, GeoJsonProperties] = Feature(
          geometry = Point(js.Array(p.longitude, p.latitude)),
          properties = StringDictionary.empty)
        mapLibre.getSource(track.id.sourceName).map {
          (geo: GeoJSONSource) => geo.setData(data)
        }
      }
    }
    positionDucks

extension (position:Position)
  def toGeoPoint: GeoPoint =
    GeoPoint(
      latitude = position.coords.latitude,
      longitude = position.coords.longitude,
      timestamp = position.timestamp.toLong
    )

extension (duckId:DuckId)
  def imageName = s"image${duckId.v}"

  def sourceName = s"source${duckId.v}"

  def layerName = s"layer${duckId.v}"
