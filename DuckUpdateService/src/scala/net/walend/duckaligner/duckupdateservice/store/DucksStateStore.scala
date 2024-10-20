package net.walend.duckaligner.duckupdateservice.store

import cats.effect.{Async, IO}
import cats.effect.std.AtomicCell
import org.http4s.client.dsl.Http4sClientDsl
import cats.syntax.all.*
import net.walend.duckaligner.duckupdates.v0.{DuckSitRepUpdate, DuckUpdate, DuckUpdateService, UpdatePositionOutput}

/**
 * @author David Walend
 * @since v0.0.0
 */
/* todo tagless final - but not on the airplane
trait DucksStateStore[F[_]]:
  / **
   * @param updatePosition updated position from a duck
   * @return the tracks for the rest of the ducks
   * /
  def updated(updatePosition: UpdatePosition):F[DucksState]

object DucksStateStore: //todo What's the minimum F to get flatMap on the Atomic cell?
  def apply[F[_]: Async](using ev:DucksStateStore[F]): DucksStateStore[F] = ev

  def ducksStateStore[F[_]: Async]:DucksStateStore[F] = new DucksStateStore[F]:
    private val ducksStateCell: F[AtomicCell[F, DucksState]] = AtomicCell[F].of(DucksState.start)

    def updated(updatePosition: UpdatePosition):F[DucksState] =
      ducksStateCell.flatMap(_.updateAndGet{ t => t.updated(updatePosition) })
*/
object DucksStateStore extends DuckUpdateService[IO]:
  private val ducksStateCell: IO[AtomicCell[IO, DucksState]] = AtomicCell[IO].of(DucksState.start)

  //todo why is positionUpdate optional? How do I make smithy require the body?
  override def updatePosition(positionUpdate: Option[DuckUpdate]): IO[UpdatePositionOutput] =
    def sitRepForPosition(duckUpdate:DuckUpdate): IO[DuckSitRepUpdate] =
      val updatePosition = UpdatePosition(duckUpdate)
      ducksStateCell.flatMap(_.updateAndGet{ t => t.updated(updatePosition) }).map(_.toDuckSitRepUpdate)
      
    positionUpdate.map(sitRepForPosition(_)).
      map(ios => ios.map(s => UpdatePositionOutput(Option(s)))).
      getOrElse(IO(UpdatePositionOutput(None)))
