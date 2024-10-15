package net.walend.sharelocationservice.store

import scala.collection.immutable.Map

/**
 * Shared structures between the ShareLocationService and the front end.
 *
 * @author David Walend
 * @since v0.0.0
 */
//todo needs to be moved to a new shared protocol subproject
final case class Tracks(protocolVersion:Int = protocolVersion,itemVersion:Int,tracks: Map[DuckId,Track]):
  def updated(updatePosition: UpdatePosition):Tracks =
    val updatedTracks = tracks.updatedWith(updatePosition.id){maybeTrack =>
      Option(maybeTrack.getOrElse(Track(updatePosition.id,Seq.empty))
        .updated(updatePosition.geoPoint))
    }
    this.copy(itemVersion = this.itemVersion + 1,tracks = updatedTracks)

object Tracks:
  def start: Tracks = Tracks(itemVersion = 0, tracks = Map.empty)

final case class UpdatePosition(protocolVersion:Int,itemVersion:Int,id: DuckId,geoPoint: GeoPoint)

object UpdatePosition:
  def unapply(string:String):Option[UpdatePosition] = ???

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
                     altitude:Double,
                     speed:Double,
                     heading:Double,
                     accuracy:Double,
                     altitudeAccuracy:Double,
                     timestamp:Long
                   )

val protocolVersion = 0