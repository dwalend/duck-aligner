package net.walend.duckaligner.duckupdateservice.store

import scala.collection.immutable.Map
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckInfo, DuckSitRepUpdate, DuckUpdate, GeoPoint, Track}
/**
 * Shared structures between the ShareLocationService and the front end.
 *
 * @author David Walend
 * @since v0.0.0
 */
//todo remove snapshot, tracks, ducks
final case class DucksState private(snapshot:Int, events:List[DuckEvent], tracks: Map[DuckId,Track], ducks: Map[DuckId,DuckInfo]):

  //todo recover from missing events
  //events are numbered. Proposed event number is next from the client's point of view N
  // server's behavior is
  //1) accept event - likely with the wrong number n+1 - and reply with events N through M\
  //1a) client restart recovery: if M = 1 then the client gets all the events the server knows
  //2) server error recovery: observe if the client knows more events than the server - and request any unknown events 1 through N -1
  //3) client error recovery: client updates its event list with the new events
  //4) server error recovery: client proposes any events it knows that the server requested

  def updateEvents(proposedEvents:List[DuckEvent]):DucksState =
    val lastEventNumber = events match {
      case List() => 0
      case _ => events.maxBy(_.order).order
    }
    //todo assumes all is well. Tack the new events on the end of the list
    val reordered = proposedEvents.zipWithIndex.map(z => z._1.withOrder(lastEventNumber + z._2 + 1))
    val updatedEvents = events.appendedAll(reordered)

    this.copy(snapshot = this.snapshot +1, events = updatedEvents)

  def eventsToClient(proposedEvents:List[DuckEvent]): (List[DuckEvent], List[Nothing]) =

    val clientKnowsEventsUpTo: Int = proposedEvents.head.order - 1
    val eventsToClient = events.drop(clientKnowsEventsUpTo)
    (eventsToClient,List.empty)


  def updatedDuckInfo(duckInfo: DuckInfo):DucksState =
    val updatedDucks = ducks.updated(duckInfo.id,duckInfo)
    this.copy(snapshot = this.snapshot + 1, ducks = updatedDucks)

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

extension(duckEvent:DuckEvent)
  def order:Int = duckEvent match {
    case DuckEvent.PositionCase(position) => position.order
    case DuckEvent.InfoCase(info) => info.order
  }

  def withOrder(order:Int):DuckEvent = duckEvent match {
    case DuckEvent.PositionCase(position) => DuckEvent.position(position.copy(order = order))
    case DuckEvent.InfoCase(info) => DuckEvent.info(info.copy(order = order))
  }

object DucksState:
  def start: DucksState = DucksState(snapshot = 0, events = List.empty, tracks = Map.empty, ducks = Map.empty)

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