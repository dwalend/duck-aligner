package net.walend.duck.front

import cats.effect.implicits.*
import cats.implicits.*
import cats.effect.{Async, Resource}
import cats.effect.std.AtomicCell
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckPositionEvent, DuckUpdateService, GeoPoint, ProposeEventsOutput}

/**
 * @author David Walend
 * @since v0.0.0
 */
case class EventStore[F[_]: Async](cell:AtomicCell[F,List[DuckEvent]]):

  private def sendPosition(position: GeoPoint, client: DuckUpdateService[F], duckId: DuckId,order:Int): F[ProposeEventsOutput] =
    val duckPositionEvent = DuckPositionEvent(
      order = order,
      id = duckId,
      position = position
    )
    client.proposeEvents(List(DuckEvent.position(duckPositionEvent)))

  private def insertEvents(eventsToInsert: List[DuckEvent]): F[List[DuckEvent]] =
    cell.updateAndGet { currentEvents =>
      val allEvents = currentEvents.appendedAll(eventsToInsert)
      allEvents
    }

  private def nextNumber: F[Int] =
    cell.get.map {
      case Seq() => 0
      case currentEvents => currentEvents.maxBy(_.order).order + 1
    }

  def sendPositionAndGetUpdates(position: GeoPoint, client: DuckUpdateService[F], duckId: DuckId):F[List[DuckEvent]] =
    for
      next <- nextNumber
      peo <- sendPosition(position,client,duckId,next)
      allEvents <- insertEvents(peo.updates)
    yield allEvents



object EventStore:
  def create[F[_]: Async]():Resource[F,EventStore[F]] =
    val cell: F[AtomicCell[F, List[DuckEvent]]] = AtomicCell[F].of(List.empty[DuckEvent])
    cell.map(c => EventStore(c)).toResource

//todo common library
extension (duckEvent: DuckEvent)
  def order: Int = duckEvent match
    case DuckEvent.PositionCase(position) => position.order
    case DuckEvent.InfoCase(info) => info.order

