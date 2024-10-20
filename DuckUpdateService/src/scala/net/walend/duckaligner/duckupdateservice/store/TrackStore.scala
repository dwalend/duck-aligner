package net.walend.duckaligner.duckupdateservice.store

import cats.effect.Async
import cats.effect.std.AtomicCell
import org.http4s.client.dsl.Http4sClientDsl
import cats.syntax.all.*

/**
 * @author David Walend
 * @since v0.0.0
 */

trait TrackStore[F[_]]:
  /**
   * @param updatePosition updated position from a duck
   * @return the tracks for the rest of the ducks
   */
  def updated(updatePosition: UpdatePosition):F[DucksState]

object TrackStore:
  def apply[F[_]](using ev:TrackStore[F]): TrackStore[F] = ev

  def trackStore[F[_]: Async]:TrackStore[F] = new TrackStore[F]:
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    import dsl.*

    private val tracksCell: F[AtomicCell[F, DucksState]] = AtomicCell[F].of(DucksState.start)

    def updated(updatePosition: UpdatePosition):F[DucksState] =
      tracksCell.flatMap(_.updateAndGet{ t => t.updated(updatePosition) })

    def getTracks:F[DucksState] = tracksCell.flatMap(_.get)