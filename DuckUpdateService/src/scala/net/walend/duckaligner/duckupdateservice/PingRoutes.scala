package net.walend.duckaligner.duckupdateservice

import cats.MonadThrow
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

/**
 * Serve static files
 *
 * @author David Walend
 * @since v0.0.0
 */
object PingRoutes {
  def pingRoutes[F[_]: MonadThrow]:HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case _@GET -> Root / "bing"  =>
        Ok("bong")
      case _@GET -> Root  =>
        Ok("root")
    }
}
