package net.walend.duckaligner.duckupdateservice.store

import cats.effect.Concurrent
import cats.effect.std.AtomicCell
import cats.syntax.all.*
import net.walend.duckaligner.duckupdates.v0.{DuckUpdate, DuckUpdateService, UpdatePositionOutput}

/**
 * @author David Walend
 * @since v0.0.0
 */
trait DucksStateStore[F[_]] extends DuckUpdateService[F]


object DucksStateStore:
  def ducksStateStore[F[_]](using ducksStateStore:DucksStateStore[F]):DucksStateStore[F] = ducksStateStore

  def makeDuckStateStore[F[_]: Concurrent]:F[DucksStateStore[F]] =
    AtomicCell[F].of(DucksState.start).map{ducksStateCell =>
      new DucksStateStore[F]:
        override def updatePosition(positionUpdate: DuckUpdate): F[UpdatePositionOutput] =
          val updatePosition = UpdatePosition(positionUpdate)
          ducksStateCell.updateAndGet { ducksState =>
              ducksState.updated(updatePosition)
            }.map { ducksState =>
              println(ducksState)
              UpdatePositionOutput(ducksState.toDuckSitRepUpdate)
            }
    }
