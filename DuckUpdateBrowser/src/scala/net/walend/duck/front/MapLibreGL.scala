package net.walend.duck.front

import cats.effect.implicits.*
import cats.effect.kernel.Temporal
import cats.effect.std.{AtomicCell, Console}
import cats.effect.{Async, IO, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckInfo, DuckUpdateService, GeoPoint}
import cats.implicits.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import typings.maplibreGl.global.maplibregl.{Marker, Map as MapLibreMap}
import typings.maplibreGl.mod.{MapOptions, MarkerOptions}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js

case class MapLibreDuckView(
                                          mapLibreMap: MapLibreMap,
                                          cell:AtomicCell[IO,Map[DuckId,MarkerAndElement]],
                                          document: org.scalajs.dom.html.Document,
                                        ):
  def updateMapLibre(sitRep: SitRep, now: Duration): IO[Unit] =
    val duckInfos: Iterable[DuckInfo] = sitRep.ducksToEvents.keys

    //add markers for any new ducks
    val addAndGetDucks: IO[Map[DuckId, MarkerAndElement]] = cell.updateAndGet{ ducksToMarkers =>
      val addDucksFor = duckInfos.filterNot(di => ducksToMarkers.keys.toSet.contains(di.id))
      val addMarkers = addDucksFor.map{di =>
        val div: HTMLElement = document.createElement("div").asInstanceOf[HTMLElement]
        div.setAttribute("id",di.id.toString)
/*
        val img = document.createElement("img").asInstanceOf[HTMLElement]
        img.setAttribute("src","https://upload.wikimedia.org/wikipedia/commons/7/7c/201408_cat.png")
        img.setAttribute("width","50")
        img.setAttribute("height","50")
        div.appendChild(img)
*/

        val p = document.createElement("p").asInstanceOf[HTMLElement]
        p.textContent = di.duckName
        div.appendChild(p)

/*
        div.innerHTML =
          """ <svg height="24" width="24" xmlns="http://www.w3.org/2000/svg">
            |  <circle r="10" cx="12" cy="12" fill="red" />
            |</svg> 
            |""".stripMargin
*/

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
      duckInfos.map{ di =>
        val p: GeoPoint = sitRep.bestPositionOf(di)
        val age = (now.toMillis - p.timestamp) / 1000
        val labelText = s"${di.duckName}\n${age}s"
        val marker = ducksToMarkers(di.id).marker
        marker.setLngLat((p.longitude, p.latitude))
        val element = ducksToMarkers(di.id).element
        element.innerHTML = ""
//        element.textContent = labelText
        //todo detect if anything interesting has changed before drawing to avoid blinking
        SvgDuck.duckSvg(di,age) //todo this should be an IO, but it's very in-the-middle of not-IO , and probably needs to happen in the update
      }.toSeq.sequence
    }.void

case class MarkerAndElement(marker:Marker,element: HTMLElement)

object MapLibreDuckView:
  def create(
                           mapLibreMap: MapLibreMap,
                           document: org.scalajs.dom.html.Document,
                         ): Resource[IO, MapLibreDuckView] =
    val cell: IO[AtomicCell[IO, Map[DuckId, MarkerAndElement]]] = AtomicCell[IO].of(Map.empty[DuckId,MarkerAndElement])
    cell.map(c => MapLibreDuckView(mapLibreMap,c,document)).toResource

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