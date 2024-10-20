package net.walend.duckaligner.duckupdateservice.store

import scala.collection.immutable.Map

import net.walend.duckaligner.duckupdates.v0.{DuckUpdate,GeoPoint => DuckPoint}
/**
 * Shared structures between the ShareLocationService and the front end.
 *
 * @author David Walend
 * @since v0.0.0
 */
final case class DucksState(snapshot:Int, tracks: Map[DuckId,Track]):
  def updated(updatePosition: UpdatePosition):DucksState =
    val updatedTracks = tracks.updatedWith(updatePosition.id){maybeTrack =>
      Option(maybeTrack.getOrElse(Track(updatePosition.id,Seq.empty))
        .updated(updatePosition.geoPoint))
    }
    this.copy(snapshot = this.snapshot + 1,tracks = updatedTracks)

object DucksState:
  def start: DucksState = DucksState(snapshot = 0, tracks = Map.empty)

final case class UpdatePosition(snapshot:Int, id: DuckId, geoPoint: GeoPoint)

object UpdatePosition:
  def apply(duckUpdate:DuckUpdate):UpdatePosition =
    UpdatePosition(duckUpdate.snapshot,new DuckId(duckUpdate.id.v),GeoPoint(duckUpdate.position))

final case class Track(id:DuckId,positions:Seq[GeoPoint]):
  def updated(geoPoint: GeoPoint): Track =
    this.copy(positions = positions.prepended(geoPoint))

final class DuckId(val v:Long) extends AnyVal

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
                   )

object GeoPoint:
  def apply(dp:DuckPoint):GeoPoint = new GeoPoint(dp.latitude,dp.longitude,dp.timestamp)