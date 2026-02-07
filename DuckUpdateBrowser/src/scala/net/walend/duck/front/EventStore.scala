package net.walend.duck.front

import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.{Async, Resource}
import cats.effect.std.AtomicCell
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckInfo, DuckUpdateService, GeoPoint, NewDuckEventsResponse}

/**
 * @author David Walend
 * @since v0.0.0
 */
case class EventStore[F[_]: Async](cell:AtomicCell[F,List[DuckEvent]]):

  private def insertEvents(eventsToInsert: List[DuckEvent]): F[List[DuckEvent]] =
    cell.updateAndGet { currentEvents =>
      val allEvents = currentEvents.appendedAll(eventsToInsert)
      allEvents
    }

  private def sendRescueEvents(event:DuckEvent,client: DuckUpdateService[F]): F[List[DuckEvent]] =
    for
      currentEvents <- cell.get
      _ <- client.rescueServer(currentEvents)
      peo <- client.proposeEvents(List[DuckEvent](event))
      allEvents <- insertOrRescueServer(peo.updates, event, client)
    yield allEvents

  private def insertOrRescueServer(response:NewDuckEventsResponse,event:DuckEvent,client: DuckUpdateService[F]):F[List[DuckEvent]] =
    response.accept(new NewDuckEventsResponse.Visitor{
      def eventsForClient(value: List[DuckEvent]):F[List[DuckEvent]] = insertEvents(value)

      def rescueServer(value: NewDuckEventsResponse.RescueServerCase.type):F[List[DuckEvent]] = sendRescueEvents(event,client)
    })

  private def nextNumber: F[Int] =
    cell.get.map {
      case Seq() => 1
      case currentEvents => currentEvents.maxBy(_.order).order + 1
    }

  def sendPositionAndGetUpdates(position: GeoPoint, client: DuckUpdateService[F], duckId: DuckId):F[List[DuckEvent]] =
    for
      event <- nextNumber.map(DuckEvent.duckPositionEvent(_,duckId,position))
      peo <- client.proposeEvents(List[DuckEvent](event))//todo throws main.js:3830 scala.scalajs.js.JavaScriptException: TypeError: Failed to fetch
      allEvents <- insertOrRescueServer(peo.updates,event,client)
    yield allEvents

  private def createDuckInfo(duckName:String): DuckInfo =
    DuckInfo(
      id = DuckId(duckName.hashCode),
      duckName = duckName,
      lastChanged = 0L //todo don't worry - this all moves outside
    )

  def sendDuckInfo(duckName:String,client: DuckUpdateService[F]):F[DuckId] =
    val duckInfo = createDuckInfo(duckName) //todo - should get duck info from central server instead - and prefetch on the duck line server
    for
      duckInfoEvent <- nextNumber.map(DuckEvent.duckInfoEvent(_, duckInfo.id, duckInfo))
      peo <- client.proposeEvents(List[DuckEvent](duckInfoEvent))
      _ <- insertOrRescueServer(peo.updates,duckInfoEvent,client)
    yield duckInfo.id
    
  def allEvents:F[List[DuckEvent]] =
    cell.get

object EventStore:
  def create[F[_]: Async]():Resource[F,EventStore[F]] =
    val cell: F[AtomicCell[F, List[DuckEvent]]] = AtomicCell[F].of(List.empty[DuckEvent])
    cell.map(c => EventStore(c)).toResource
