package net.walend.duck.front

import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckInfo, GeoPoint}
import net.walend.duckaligner.duckupdates.v0.DuckEvent.{DuckInfoEvent, DuckPositionEvent}

/**
 * @param order the event count used to create this SitRep
 * @param ducksToEvents a map from the most recent duck info to the events for that duck - most recent first
 * 
 * @author David Walend
 * @since v0.0.0
 */
case class SitRep(order:Int,ducksToEvents:Map[DuckInfo,Seq[DuckEvent]]): 
  def bestPositionOf(duckInfo: DuckInfo): GeoPoint =
    ducksToEvents.get(duckInfo)
      .flatMap(_.collectFirst { case p: DuckPositionEvent => p.position })
      .get //todo handle no position - probably return unknown position

object SitRep:
  def apply(events:List[DuckEvent]):SitRep = {
    val order: Int = events match {
      case Seq() => -1  //todo probably an error case
      case _ => events.maxBy(_.order).order
    }
    val ducksToEvents: Map[DuckInfo, Seq[DuckEvent]] = 
      events.groupBy(_.id).map{ (idAndEvents: (DuckId, Seq[DuckEvent])) =>
        val bestDuckInfo: DuckInfo = idAndEvents._2
          .collect{case info:DuckInfoEvent => info}
          .match
            case Seq() => unknownDuck(idAndEvents._1)
            case infos => infos.maxBy(_.order).duckInfo
        val duckEvents = idAndEvents._2.sortBy(_.order).reverse 
        bestDuckInfo -> duckEvents
      }
    SitRep(order,ducksToEvents)  
  }
  
  private def unknownDuck(duckId: DuckId): DuckInfo = DuckInfo(duckId,duckName = "MysteryDuck",0L)
  


