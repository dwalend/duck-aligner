package net.walend.duckaligner.duckupdateservice.store

import scala.collection.immutable.Map

//todo who gets the Duck prefix? (Change DuckI)
import net.walend.duckaligner.duckupdates.v0.{DuckUpdate,GeoPoint => DuckPoint,DuckSitRepUpdate,Track => DuckTrack,DuckId => DuckI}
/**
 * Shared structures between the ShareLocationService and the front end.
 *
 * @author David Walend
 * @since v0.0.0
 */
final case class DucksState(snapshot:Int, tracks: Map[DuckId,Track]):
  def updated(updatePosition: UpdatePosition):DucksState =
    val updatedTracks = tracks.updatedWith(updatePosition.id){maybeTrack =>
      Option(
        maybeTrack.getOrElse(Track(updatePosition.id,Seq.empty))
        .updated(updatePosition.geoPoint)
      )
    }
    this.copy(snapshot = this.snapshot + 1,tracks = updatedTracks)

  def toDuckSitRepUpdate:DuckSitRepUpdate =
    DuckSitRepUpdate(
      snapshot = snapshot,
      tracks = tracks.map((d,t) => d.toSmithyMapKey -> t.toDuckTrack),
    )


object DucksState:
  def start: DucksState = DucksState(snapshot = 0, tracks = Map.empty)

final case class UpdatePosition(snapshot:Int, id: DuckId, geoPoint: GeoPoint)

object UpdatePosition:
  def apply(duckUpdate:DuckUpdate):UpdatePosition =
    UpdatePosition(duckUpdate.snapshot,new DuckId(duckUpdate.id.v),GeoPoint(duckUpdate.position))

final case class Track(id:DuckId,positions:Seq[GeoPoint]):
  def updated(geoPoint: GeoPoint): Track =
    this.copy(positions = positions.prepended(geoPoint))

  def toDuckTrack:DuckTrack = DuckTrack(id.toDuckI,positions.map(_.toDuckPoint).toList)

final class DuckId(val v:Long) extends AnyVal:
  def toSmithyMapKey: String = v.toString

  def toDuckI: DuckI = DuckI(v)

/**
 * Mostly from org.scalajs.dom.{Geolocation, Position, PositionError, document}'s idea of coordinates
 */
final case class GeoPoint(
                     latitude:Double,
                     longitude:Double,
//                     altitude:Double,
//                     speed:Double,
//                     heading:Double,
//                     accuracy:Double,
//                     altitudeAccuracy:Double,
                     timestamp:Long
                   ):
  def toDuckPoint:DuckPoint = DuckPoint(latitude,longitude, timestamp)

object GeoPoint:
  def apply(dp:DuckPoint):GeoPoint = new GeoPoint(dp.latitude,dp.longitude,dp.timestamp)