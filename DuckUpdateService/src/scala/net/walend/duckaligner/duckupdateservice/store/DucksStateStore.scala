package net.walend.duckaligner.duckupdateservice.store

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.syntax.all.*
import net.walend.duckaligner.duckupdates.v0.{DuckUpdate, DuckUpdateService, MapLibreGlKeyOutput, UpdatePositionOutput}
import net.walend.duckaligner.duckupdateservice.Log
import net.walend.duckaligner.duckupdateservice.awssdklocation.AwsSecrets


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
        override def updatePosition(positionUpdate: DuckUpdate): F[UpdatePositionOutput] =
          val updatePosition = UpdatePosition(positionUpdate)
          ducksStateCell.updateAndGet { ducksState =>
              ducksState.updated(updatePosition)
            }.map { ducksState =>
              Log.log(ducksState.toDuckSitRepUpdate.toString)
              UpdatePositionOutput(ducksState.toDuckSitRepUpdate)
            }

        override def mapLibreGlKey(): F[MapLibreGlKeyOutput] = Async[F].pure(MapLibreGlKeyOutput(AwsSecrets.apiKey))
    }
