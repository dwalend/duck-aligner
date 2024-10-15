package net.walend.sharelocationservice.store

import cats.effect.Async
import org.http4s.client.dsl.Http4sClientDsl

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */

trait TrackStore[F[_]]:
  /**
   * @param updatePosition updated position from a duck
   * @return the tracks for the rest of the ducks
   */
  def updated(updatePosition: UpdatePosition):F[Tracks]

object TrackStore:
  def apply[F[_]](using ev:TrackStore[F]): TrackStore[F] = ev

  def trackStore[F[_]: Async]:TrackStore[F] = new TrackStore[F]:
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    import dsl.*

    //todo single-threaded access (in IO)
    private var tracks: Tracks = Tracks.start
    
    def updated(updatePosition: UpdatePosition):F[Track] =
      tracks = tracks.updated(updatePosition)
      tracks
      ???
    
    def getTracks:Tracks = tracks