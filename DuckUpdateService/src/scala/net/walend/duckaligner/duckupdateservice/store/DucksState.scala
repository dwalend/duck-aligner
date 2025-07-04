package net.walend.duckaligner.duckupdateservice.store

import net.walend.duckaligner.duckupdates.v0.DuckEvent
/**
 * Shared structures between the ShareLocationService and the front end.
 *
 * @author David Walend
 * @since v0.0.0
 */
//todo remove snapshot, tracks, ducks
final case class DucksState private(snapshot:Int, events:List[DuckEvent]):

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

  def eventsToClient(proposedEvents:List[DuckEvent]): List[DuckEvent] =
    val clientKnowsEventsUpTo: Int = proposedEvents.head.order - 1
    val eventsForClient = events.drop(clientKnowsEventsUpTo)
    eventsForClient
    
  def restartedNeedsEvents(proposedEvents:List[DuckEvent]):Boolean = 
    //If the events list is empty due to restart 
    //and the client knows more than the proposed events 
    //then the server needs the client's events
    events.isEmpty && proposedEvents.head.order != 1

  def rescue(proposedEvents: List[DuckEvent]): DucksState =
    if (events.isEmpty) this.copy(snapshot = this.snapshot + 1, events = proposedEvents)
    else this 

extension(duckEvent:DuckEvent)
  def withOrder(order:Int):DuckEvent =
    duckEvent.accept(new DuckEvent.Visitor[DuckEvent]:
      def position(value: DuckEvent.DuckPositionEvent): DuckEvent = value.copy(order = order)
      def info(value: DuckEvent.DuckInfoEvent): DuckEvent = value.copy(order = order)
    )

object DucksState:
  def start: DucksState = DucksState(snapshot = 0, events = List.empty)

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