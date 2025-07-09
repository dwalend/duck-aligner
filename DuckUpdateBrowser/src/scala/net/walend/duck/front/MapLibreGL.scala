package net.walend.duck.front

import cats.effect.implicits.effectResourceOps
import cats.effect.kernel.Temporal
import cats.effect.std.{AtomicCell, Console}
import cats.effect.{Async, IO, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckInfo, DuckUpdateService, GeoPoint}
import cats.implicits.*
import typings.maplibreGl.global.maplibregl.{Marker, Map as MapLibreMap}
import typings.maplibreGl.mod.MapOptions

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js

//todo create a case class that has a MapLibreMap and a map from duckIds to Markers
//todo put a mutable map in an atomic cell
case class MapLibreDuckView[F[_]: Async](mapLibreMap: MapLibreMap,cell:AtomicCell[F,Map[DuckId,Marker]]):
  def updateMapLibre(sitRep: SitRep, now: Duration): F[Unit] =
    val duckInfos: Iterable[DuckInfo] = sitRep.ducksToEvents.keys

    //add markers for any new ducks
    val addAndGetDucks: F[Map[DuckId, Marker]] = cell.updateAndGet{ ducksToMarkers =>
      val addDucksFor = duckInfos.filterNot(di => ducksToMarkers.keys.toSet.contains(di.id))
      val addMarkers = addDucksFor.map{di =>
        val marker = Marker()
        val p: GeoPoint = sitRep.bestPositionOf(di)
        val age = (now.toMillis - p.timestamp) / 1000
        val labelText = s"${di.duckName}\n${age}s"
        marker.setLngLat((p.longitude, p.latitude))
        marker.addTo(mapLibreMap)
        println(s"Added $marker for ${di.id} $labelText")
        di.id -> marker
      }
      ducksToMarkers.concat(addMarkers)
    }

    //update all the locations for all the ducks
    addAndGetDucks.map{ ducksToMarkers =>
      duckInfos.map{ di =>
        val p: GeoPoint = sitRep.bestPositionOf(di)
        val age = (now.toMillis - p.timestamp) / 1000
        val labelText = s"${di.duckName}\n${age}s"
        val marker = ducksToMarkers(di.id)
        marker.setLngLat((p.longitude, p.latitude))
      }
    }.void

object MapLibreDuckView:
  def create[F[_]: Async](mapLibreMap: MapLibreMap): Resource[F, MapLibreDuckView[F]] =
    val cell: F[AtomicCell[F, Map[DuckId, Marker]]] = AtomicCell[F].of(Map.empty[DuckId,Marker])
    cell.map(c => MapLibreDuckView(mapLibreMap,c)).toResource

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