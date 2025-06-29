package net.walend.duck.front

import cats.effect.kernel.Temporal
import cats.effect.std.Console
import cats.effect.{Async, IO, Resource}
import org.scalajs.dom.HTMLImageElement
import net.walend.duckaligner.duckupdates.v0.{DuckInfo, DuckUpdateService, GeoPoint}
import typings.geojson.mod.{Feature, FeatureCollection, GeoJSON, GeoJsonProperties, Geometry, Point}
import cats.implicits.*
import org.scalablytyped.runtime.StringDictionary
import typings.maplibreGl.global.maplibregl.{GeoJSONSource, Map as MapLibreMap}
import typings.maplibreGl.mod.{GetResourceResponse, MapOptions}
import typings.maplibreMaplibreGlStyleSpec.anon.Iconallowoverlap
import typings.maplibreMaplibreGlStyleSpec.maplibreMaplibreGlStyleSpecStrings.top
import typings.maplibreMaplibreGlStyleSpec.mod.{GeoJSONSourceSpecification, LayerSpecification, SourceSpecification}
import typings.std.ImageBitmap

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js

object MapLibreGL:
  //todo make tagless final after GeoIO is unstuck from IO
  def mapLibreResource(geoIO: GeoIO, client: DuckUpdateService[IO]): Resource[IO, MapLibreMap] =

    def mapLibreF[F[_] : Async](apiKey: String, c: GeoPoint): F[MapLibreMap] =
      val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
      val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.

      val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"
      Async[F].blocking {
        new MapLibreMap(new MapOptions {
          style = styleUrl
          var container = "map"
          center = (c.longitude, c.latitude)
          zoom = 7 //7 is about a 3-hour drive from the center
        })
      }

    def waitToLoad[F[_] : Async : Console](mapLibreMap: MapLibreMap):F[Unit] = {
      Async[F].blocking(mapLibreMap.loaded())
        .flatMap{ loaded =>
          if(loaded) Async[F].unit
          else Console[F].println(s"Map not yet loaded.") *> Temporal[F].sleep(200.milliseconds) *> waitToLoad(mapLibreMap)
        }
    }

    val ml: IO[MapLibreMap] = for {
      apiKey <- client.mapLibreGlKey().map(_.key)
      c <- geoIO.position()
      mapLibre <- mapLibreF[IO](apiKey,c)
      _ <- waitToLoad[IO](mapLibre)
    } yield mapLibre
    ml.toResource

  def updateMapLibre[F[_] : Async](mapLibre: MapLibreMap, sitRep: SitRep, now:Duration): F[Unit] =
    val duckInfos = sitRep.ducksToEvents.keys

    //filter the list of ducks for ducks that don't have images
    val imageNames: js.Array[String] = mapLibre.listImages()
    val newDucks: Seq[DuckInfo] = duckInfos.filterNot(d => imageNames.contains(d.imageName)).toSeq

    val addNewDucksF = newDucks.map { d =>
      //add an image for each of those ducks
      val imageName = d.imageName
      val loadDuckImage = Async[F].fromFuture(Async[F].blocking(mapLibre.loadImage("https://upload.wikimedia.org/wikipedia/commons/7/7c/201408_cat.png")
        .toFuture))
      val addImage: F[mapLibre.type] = loadDuckImage.map((i: GetResourceResponse[HTMLImageElement | ImageBitmap]) => i.data match {
        case element: HTMLImageElement => mapLibre.addImage(imageName, element)
        case bitmap => mapLibre.addImage(imageName, bitmap.asInstanceOf[typings.std.global.ImageBitmap])
      })

      val p: GeoPoint = sitRep.bestPositionOf(d) //todo handle no position - probably just bail out before even loading an image, or put the duck in a UFO
      val featureSpec: GeoJSONSourceSpecification = SourceSpecification.GeoJSONSourceSpecification(FeatureCollection(
        js.Array(Feature(Point(js.Array(p.longitude, p.latitude)), ""))
      ))
      val sourceName = d.sourceName
      val layerName = d.layerName

      val age = (now.toMillis - sitRep.bestPositionOf(d).timestamp)/1000
      val labelText = s"${d.duckName}\n${age}s"

      addImage.map { _ =>
        mapLibre.addSource(sourceName, featureSpec)
        val layerSpec = LayerSpecification
          .SymbolLayerSpecification(layerName, sourceName)
          .setLayout(
            Iconallowoverlap()
              .`setIcon-allow-overlap`(true)
              .`setIcon-image`(imageName)
              .`setIcon-size`(0.125)
              .`setText-field`(labelText)
              .`setText-offset`((0d, 1.25))
              .`setText-anchor`(top)
          )
        mapLibre.addLayer(layerSpec)
      }
    }.sequence

    //for each duck get the layer spec, move the feature spec
    val positionDucks = addNewDucksF.map { _ =>
      sitRep.ducksToEvents.keys.map { d =>
        val p: GeoPoint = sitRep.bestPositionOf(d)
        val data: GeoJSON[Geometry, GeoJsonProperties] = Feature(
          geometry = Point(js.Array(p.longitude, p.latitude)),
          properties = StringDictionary.empty
        )
        mapLibre.getSource(d.sourceName).map {
          (geo: GeoJSONSource) => geo.setData(data)
        }
      }
    }
    positionDucks.void

  extension (duckInfo:DuckInfo)
    private def imageName = s"image${duckInfo.id.v}"
  
    private def sourceName = s"source${duckInfo.id.v}"
  
    private def layerName = s"layer${duckInfo.id.v}"