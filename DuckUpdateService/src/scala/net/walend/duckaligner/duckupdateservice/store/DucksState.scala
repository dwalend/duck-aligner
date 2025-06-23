package net.walend.duckaligner.duckupdateservice.store

import scala.collection.immutable.Map

import net.walend.duckaligner.duckupdates.v0.{DuckInfo, DuckUpdate,GeoPoint,DuckSitRepUpdate,Track,DuckId}
/**
 * Shared structures between the ShareLocationService and the front end.
 *
 * @author David Walend
 * @since v0.0.0
 */
final case class DucksState private(snapshot:Int, tracks: Map[DuckId,Track], ducks: Map[DuckId,DuckInfo]):
//todo use this
  def updatedDuckInfo(duckInfo: DuckInfo):DucksState = {
    val updatedDucks = ducks.updated(duckInfo.id,duckInfo)
    this.copy(snapshot = this.snapshot + 1, ducks = updatedDucks)
  }

  def updatedPosition(updatePosition: UpdatePosition):DucksState =
//todo easy cleanup when duck info is from a persistent store or startup info
    ducks.get(updatePosition.id).map { di =>
      val updatedTracks: Map[DuckId, Track] = tracks.updatedWith(updatePosition.id) { maybeTrack =>
        Option(
          maybeTrack.getOrElse(Track(di, List.empty))
            .updated(updatePosition.geoPoint)
        )
      }
      this.copy(snapshot = this.snapshot + 1, tracks = updatedTracks)
    }.getOrElse{
      val duckInfo = DuckInfo(updatePosition.id,updatePosition.duckName,0L)

      val hasDuck = updatedDuckInfo(duckInfo)
      val updatedTracks: Map[DuckId, Track] = tracks.updatedWith(updatePosition.id) { maybeTrack =>
        Option(
          maybeTrack.getOrElse(Track(duckInfo, List.empty))
            .updated(updatePosition.geoPoint)
        )
      }
      hasDuck.copy(snapshot = this.snapshot + 1, tracks = updatedTracks)
    }

  def toDuckSitRepUpdate:DuckSitRepUpdate =
    DuckSitRepUpdate(
      snapshot = snapshot,
      tracks = tracks.values.toList.sortBy(_.positions.head.timestamp)
    )


object DucksState:
  def start: DucksState = DucksState(snapshot = 0, tracks = Map.empty, ducks = Map.empty)

final case class UpdatePosition(duckName:String, id: DuckId, geoPoint: GeoPoint)

object UpdatePosition:
  def apply(duckUpdate:DuckUpdate):UpdatePosition =
    UpdatePosition(duckUpdate.duckName,new DuckId(duckUpdate.id.v),duckUpdate.position)

extension(track:Track)
  def updated(geoPoint: GeoPoint): Track =
    track.copy(positions = track.positions.prepended(geoPoint))
/*
 * Mostly from org.scalajs.dom.{Geolocation, Position, PositionError, document}'s idea of coordinates

* final case class GeoPoint(
                     latitude:Double,
                     longitude:Double,
//                     altitude:Double,
//                     speed:Double,
//                     heading:Double,
//                     accuracy:Double,
//                     altitudeAccuracy:Double,
                     timestamp:Long
                   ):
*/