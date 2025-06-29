package net.walend.duckaligner.duckupdateservice.store

import cats.syntax.all.*
import cats.effect.Async
import cats.effect.std.AtomicCell
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckUpdateService, MapLibreGlKeyOutput, NewDuckEventsResponse, ProposeEventsOutput}
import net.walend.duckaligner.duckupdateservice.awssdklocation.AwsSecrets
import org.typelevel.log4cats.slf4j.Slf4jLogger


/**
 * @author David Walend
 * @since v0.0.0
 */
case class DucksStateStore[F[_]: Async](ducksStateCell:AtomicCell[F,DucksState]) extends DuckUpdateService[F]:
  def mapLibreGlKey(): F[MapLibreGlKeyOutput] =
    Async[F].pure(MapLibreGlKeyOutput(AwsSecrets.apiKey))

  def proposeEvents(proposal: List[DuckEvent]): F[ProposeEventsOutput] = {
    ducksStateCell.get.map{ ducksState =>
      ducksState.restartedNeedsEvents(proposal)
    }.flatMap{rescue =>
      if(!rescue) updateServerAndClient(proposal)
      else Slf4jLogger.create[F]
        .flatMap(_.info(s"Requesting rescue"))
        .map(_ => ProposeEventsOutput(NewDuckEventsResponse.rescueServer()))
    }
  }

  private def updateServerAndClient(proposal: List[DuckEvent]): F[ProposeEventsOutput] = {
    ducksStateCell.updateAndGet { ducksState =>
      ducksState.updateEvents(proposal)
    }.map { duckState =>
      val forClient = duckState.eventsToClient(proposal)
      ProposeEventsOutput(NewDuckEventsResponse.eventsForClient(forClient))
    }.flatTap { events =>
      Slf4jLogger.create[F].flatMap(_.info(s"Update Events: $events Proposed: $proposal"))
    }
  }

  def rescueServer(proposal: List[DuckEvent]): F[Unit] =
    ducksStateCell.updateAndGet { ducksState =>
      ducksState.rescue(proposal)
    }.flatTap { events =>
      Slf4jLogger.create[F].flatMap(_.info(s"Rescue Events: $events Proposed: $proposal"))
    }.void  

object DucksStateStore:
  def makeDuckStateStore[F[_]: Async]:F[DucksStateStore[F]] =
    AtomicCell[F].of(DucksState.start).map{ducksStateCell =>
      DucksStateStore(ducksStateCell)
    }
