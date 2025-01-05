package net.walend.duckaligner.duckupdateservice.awssdklocation

import cats.MonadThrow
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object AwsLocationRoutes {
  //todo tagless final
  def awsLocationRoutes:HttpRoutes[IO] =
//    def awsLocationRoutes[F[_] : MonadThrow]: HttpRoutes[F] =

    val dsl = new Http4sDsl[IO]{}
    import dsl.*
    HttpRoutes.of[IO] {
      case _@GET -> Root / "awsLocation" / "mapStyle"  =>
        AwsLocationClient.requestStaticMap.flatMap(r => Ok(r.blob().toString))
    }
}
