package net.walend.duckaligner.duckupdateservice.store

import cats.effect.Async
import cats.effect.std.AtomicCell
import cats.syntax.all.*
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckInfo, DuckUpdate, DuckUpdateService, GetDuckIdOutput, MapLibreGlKeyOutput, UpdatePositionOutput}
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

        override def getDuckId(duckIdFinder: String): F[GetDuckIdOutput] = { 
          //todo real time stamp from Clock
          val duckInfo = DuckInfo(DuckId(duckIdFinder.hashCode),duckIdFinder,0L)
          ducksStateCell.updateAndGet { ducksState =>
            ducksState.updatedDuckInfo(duckInfo)
          }.as {
            GetDuckIdOutput(duckInfo.id)
          }.flatTap { _ =>
            Slf4jLogger.create[F].flatMap(_.info(s"$duckInfo"))
          }
        }

        override def updatePosition(positionUpdate: DuckUpdate): F[UpdatePositionOutput] =
          val updatePosition = UpdatePosition(positionUpdate)
          ducksStateCell.updateAndGet { ducksState =>
            ducksState.updatedPosition(updatePosition)
          }.map { ducksState =>
            UpdatePositionOutput(ducksState.toDuckSitRepUpdate)
          }.flatTap { upo =>
            Slf4jLogger.create[F].flatMap(_.info(s"${upo.sitRep}"))
          }

        override def mapLibreGlKey(): F[MapLibreGlKeyOutput] = 
          Async[F].pure(MapLibreGlKeyOutput(AwsSecrets.apiKey))
    }
