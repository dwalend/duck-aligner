package net.walend.duck.front

import cats.effect.implicits.*
import cats.effect.kernel.Temporal
import cats.effect.std.{AtomicCell, Console}
import cats.effect.{Async, IO, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckInfo, DuckUpdateService, GeoPoint}
import cats.implicits.*
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import typings.geojson.mod.{Feature, FeatureCollection, GeoJSON, GeoJsonProperties, Geometry, LineString}
import typings.maplibreGl.global.maplibregl.{GeoJSONSource, Marker, Map as MapLibreMap}
import typings.maplibreGl.mod.{MapOptions, MarkerOptions}
import typings.maplibreMaplibreGlStyleSpec.anon.{Lineblur, Linecap}
import typings.maplibreMaplibreGlStyleSpec.maplibreMaplibreGlStyleSpecStrings
import typings.maplibreMaplibreGlStyleSpec.mod.{GeoJSONSourceSpecification, LayerSpecification, SourceSpecification}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js

case class MapLibreDuckView(
                                          mapLibreMap: MapLibreMap,
                                          cell:AtomicCell[IO,Map[DuckId,MarkerAndElement]],
                                          document: org.scalajs.dom.html.Document,
                                        ):
  def updateMapLibre(sitRep: SitRep, now: Duration): IO[Unit] =
    val duckInfos: Seq[DuckInfo] = sitRep.ducksToEvents.keys.toSeq

    val lineBlur = Lineblur()
    lineBlur.`line-color` = "red"
    lineBlur.`line-width` = 8

    val lineCap = Linecap()
    lineCap.`line-cap` = maplibreMaplibreGlStyleSpecStrings.round
    lineCap.`line-join` = maplibreMaplibreGlStyleSpecStrings.round

    //add markers for any new ducks
    val addAndGetDucks: IO[Map[DuckId, MarkerAndElement]] = cell.updateAndGet{ ducksToMarkers =>
      val addedDucks = duckInfos.filterNot(di => ducksToMarkers.keys.toSet.contains(di.id))

      val addMarkers: Iterable[(DuckId, MarkerAndElement)] = addedDucks.map{ di =>
        val _ = mapLibreMap.getSource(di.sourceName).getOrElse {

          import js.JSConverters._
          val points = sitRep.positionsOf(di)
          //add all the points to the duck's source
          val positions = points.take(1).map { p => js.Array(p.longitude, p.latitude) }.toJSArray

          val featureSpec: GeoJSONSourceSpecification = SourceSpecification.GeoJSONSourceSpecification(FeatureCollection(
            js.Array(Feature(geometry = LineString(positions), properties = ""))
          ))

          mapLibreMap.addSource(di.sourceName, featureSpec)
          //add a layer for each new duck's line
          val layerSpec = LayerSpecification
            .LineLayerSpecification(di.layerName, di.sourceName)
            .setPaint(lineBlur)
            .setLayout(lineCap)
          mapLibreMap.addLayer(layerSpec)
        }

        //add a marker for each new duck's position
        val div: HTMLElement = document.createElement("div").asInstanceOf[HTMLElement]
        div.setAttribute("id",di.id.toString)

        val p = document.createElement("p").asInstanceOf[HTMLElement]
        p.textContent = di.duckName
        val _ = div.appendChild(p)
        
        val markerOptions = MarkerOptions().setElement(div)
        val marker = Marker(markerOptions)
        val point: GeoPoint = sitRep.bestPositionOf(di)
        marker.setLngLat((point.longitude, point.latitude))
        marker.addTo(mapLibreMap)
        println(s"Added $marker for ${di.id} ${di.duckName}")
        di.id -> MarkerAndElement(marker,div)
      }
      ducksToMarkers.concat(addMarkers)
    }

    //update all the locations for all the ducks
    addAndGetDucks.flatMap { ducksToMarkers =>

      val updateLines = duckInfos.map { di =>
        import js.JSConverters._
        val points = sitRep.positionsOf(di)
        //add all the points to the duck's source
        val positions = points.map{p => js.Array(p.longitude, p.latitude)}.toJSArray
        val data: GeoJSON[Geometry, GeoJsonProperties] = Feature(
          geometry = LineString(positions),
          properties = StringDictionary.empty
        )
        IO(mapLibreMap.getSource(di.sourceName).map {
          (geo: GeoJSONSource) => geo.setData(data)
        })
      }.sequence

      val updateDucks: IO[Seq[Unit]] = duckInfos.map{ di =>
        val p: GeoPoint = sitRep.bestPositionOf(di)
        val age = (now.toMillis - p.timestamp) / 1000
        val marker = ducksToMarkers(di.id).marker
        marker.setLngLat((p.longitude, p.latitude))
        val element = ducksToMarkers(di.id).element
        element.innerHTML = ""
        SvgDuck.duckSvg(di,age)
      }.sequence

      updateLines *> updateDucks.void
    }   

  extension (duckInfo: DuckInfo)
    private def sourceName = s"source${duckInfo.id.v}"

    private def layerName = s"layer${duckInfo.id.v}"

case class MarkerAndElement(
                             marker:Marker,
                             element: HTMLElement,
                             //featureSpec: GeoJSONSourceSpecification,
                           )

object MapLibreDuckView:
  def create(
                           mapLibreMap: MapLibreMap,
                           document: org.scalajs.dom.html.Document,
                         ): Resource[IO, MapLibreDuckView] =
    val cell: IO[AtomicCell[IO, Map[DuckId, MarkerAndElement]]] = AtomicCell[IO].of(Map.empty[DuckId,MarkerAndElement])
    cell.map(c => MapLibreDuckView(mapLibreMap,c,document)).toResource

object MapLibreGL:
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