package net.walend.duck.front

import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.{Async, Resource}
import cats.effect.std.AtomicCell
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckInfo, DuckUpdateService, GeoPoint, NewDuckEventsResponse, ProposeEventsOutput}

/**
 * @author David Walend
 * @since v0.0.0
 */
case class EventStore[F[_]: Async](cell:AtomicCell[F,List[DuckEvent]]):

  private def sendPosition(position: GeoPoint, client: DuckUpdateService[F], duckId: DuckId,order:Int): F[ProposeEventsOutput] =
    val duckPositionEvent = DuckEvent.duckPositionEvent(
      order = order,
      id = duckId,
      position = position
    )
    client.proposeEvents(List[DuckEvent](duckPositionEvent))

  private def sendDuckInfo(duckInfo:DuckInfo, client: DuckUpdateService[F], order: Int): F[ProposeEventsOutput] =
    val duckInfoEvent = DuckEvent.duckInfoEvent(
      order = order,
      id = duckInfo.id,
      duckInfo = duckInfo
    )
    client.proposeEvents(List[DuckEvent](duckInfoEvent))

  private def insertEvents(eventsToInsert: List[DuckEvent]): F[List[DuckEvent]] =
    cell.updateAndGet { currentEvents =>
      val allEvents = currentEvents.appendedAll(eventsToInsert)
      allEvents
    }
    
  private def insertOrRescueServer(response:NewDuckEventsResponse):F[List[DuckEvent]] =
    response.accept(new NewDuckEventsResponse.Visitor{
      def eventsForClient(value: List[DuckEvent]):F[List[DuckEvent]] = insertEvents(value)

      def eventsServerWants(value: List[Int]):F[List[DuckEvent]] = ??? //todo fill in recovery
    })

  private def nextNumber: F[Int] =
    cell.get.map {
      case Seq() => 0
      case currentEvents => currentEvents.maxBy(_.order).order + 1
    }

  def sendPositionAndGetUpdates(position: GeoPoint, client: DuckUpdateService[F], duckId: DuckId):F[List[DuckEvent]] =
    for
      next <- nextNumber
      peo <- sendPosition(position,client,duckId,next)
      allEvents <- insertOrRescueServer(peo.updates)
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
      next <- nextNumber
      peo <- sendDuckInfo(duckInfo,client,next)
      _ <- insertOrRescueServer(peo.updates)
    yield duckInfo.id

object EventStore:
  def create[F[_]: Async]():Resource[F,EventStore[F]] =
    val cell: F[AtomicCell[F, List[DuckEvent]]] = AtomicCell[F].of(List.empty[DuckEvent])
    cell.map(c => EventStore(c)).toResource
