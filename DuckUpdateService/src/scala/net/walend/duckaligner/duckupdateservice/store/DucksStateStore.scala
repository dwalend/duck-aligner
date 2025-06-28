package net.walend.duckaligner.duckupdateservice.store

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.syntax.all.*
import net.walend.duckaligner.duckupdates.v0.{DuckEvent, DuckId, DuckInfo, DuckUpdateService, MapLibreGlKeyOutput, ProposeEventsOutput}
import net.walend.duckaligner.duckupdateservice.awssdklocation.AwsSecrets
import org.typelevel.log4cats.slf4j.Slf4jLogger


/**
 * @author David Walend
 * @since v0.0.0
 */
trait DucksStateStore[F[_]] extends DuckUpdateService[F]


object DucksStateStore:
  def ducksStateStore[F[_]](using ducksStateStore:DucksStateStore[F]):DucksStateStore[F] = ducksStateStore

  def makeDuckStateStore[F[_]: Async]:F[DucksStateStore[F]] =
    AtomicCell[F].of(DucksState.start).map{ducksStateCell =>
      new DucksStateStore[F]:
        override def mapLibreGlKey(): F[MapLibreGlKeyOutput] = 
          Async[F].pure(MapLibreGlKeyOutput(AwsSecrets.apiKey))

        override def proposeEvents(proposal: List[DuckEvent]): F[ProposeEventsOutput] =
          ducksStateCell.updateAndGet{ ducksState =>
            ducksState.updateEvents(proposal)
          }.map { duckState =>
            val forClient = duckState.eventsToClient(proposal)
            ProposeEventsOutput(forClient._1,forClient._2)
          }.flatTap{ eventsAndMissing =>
            Slf4jLogger.create[F].flatMap(_.info(s"Events: ${eventsAndMissing._1} Missing ${eventsAndMissing._2} Proposed: $proposal"))
          }
    }
