package net.walend.duck.front

import cats.effect.{Async, IO, Resource}
import org.scalajs.dom.HTMLImageElement
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdateService, GeoPoint, Track, UpdatePositionOutput}
import typings.geojson.mod.{Feature, FeatureCollection, GeoJSON, GeoJsonProperties, Geometry, Point}
import cats.implicits.*
import org.scalablytyped.runtime.StringDictionary
import typings.maplibreGl.global.maplibregl.{GeoJSONSource, Map as MapLibreMap}
import typings.maplibreGl.mod.{GetResourceResponse, MapOptions}
import typings.maplibreMaplibreGlStyleSpec.anon.Iconallowoverlap
import typings.maplibreMaplibreGlStyleSpec.maplibreMaplibreGlStyleSpecStrings.top
import typings.maplibreMaplibreGlStyleSpec.mod.{GeoJSONSourceSpecification, LayerSpecification, SourceSpecification}
import typings.std.ImageBitmap

import scala.scalajs.js

object MapLibreGL:
  //todo make tagless final after GeoIO is unstuck from IO
  def mapLibreResource(geoIO: GeoIO, client: DuckUpdateService[IO]): Resource[IO, MapLibreMap] =

    def mapLibreResource(apiKey: String, c: GeoPoint): Resource[IO, MapLibreMap] =
      val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
      val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.

      val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"
      IO.blocking {
        new MapLibreMap(new MapOptions {
          style = styleUrl
          var container = "map"
          center = (c.longitude, c.latitude)
          zoom = 7 //7 is about a 3-hour drive from the center
        })
      }.toResource

    for {
      apiKey <- client.mapLibreGlKey().map(_.key).toResource
      c <- geoIO.positionResource()
      mapLibre <- mapLibreResource(apiKey,c)
    } yield {
      mapLibre
    }

  def updateMapLibre[F[_]: Async](mapLibre:MapLibreMap,update: UpdatePositionOutput): F[Unit] =
    //todo add enough data to UpdatePositionOutput to figure out the image
    val duckTracks: Seq[Track] = update.sitRep.tracks

    //filter the list of ducks for ducks that don't have images
    val imageNames: js.Array[String] = mapLibre.listImages()
    val newDucks = duckTracks.filterNot(d => imageNames.contains(d.id.imageName))
    val addNewDucks = newDucks.map{ d =>
      //add an image for each of those ducks
      val imageName = d.id.imageName
      val loadDuckImage = Async[F].fromFuture(Async[F].blocking(mapLibre.loadImage("https://upload.wikimedia.org/wikipedia/commons/7/7c/201408_cat.png")
        .toFuture))
      val addImage: F[mapLibre.type] = loadDuckImage.map((i: GetResourceResponse[HTMLImageElement | ImageBitmap]) => i.data match {
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
        val layerSpec = LayerSpecification
          .SymbolLayerSpecification(layerName, sourceName)
          .setLayout(
            Iconallowoverlap()
              .`setIcon-image`(imageName)
              .`setIcon-size`(0.125)
              .`setText-field`("DuckName")
              .`setText-offset`((0d, 1.25))
              .`setText-anchor`(top)
          )
        mapLibre.addLayer(layerSpec)
      }
    }.sequence
    //for each duck get the layer spec, move the feature spec
    val positionDucks = addNewDucks.map { _ =>
      duckTracks.map{ track =>
        val p: GeoPoint = track.positions.head
        val data:GeoJSON[Geometry, GeoJsonProperties] = Feature(
          geometry = Point(js.Array(p.longitude, p.latitude)),
          properties = StringDictionary.empty
        )
        mapLibre.getSource(track.id.sourceName).map {
          (geo: GeoJSONSource) => geo.setData(data)
        }
      }
    }
    positionDucks.void

  extension (duckId:DuckId)
    private def imageName = s"image${duckId.v}"
  
    private def sourceName = s"source${duckId.v}"
  
    private def layerName = s"layer${duckId.v}"