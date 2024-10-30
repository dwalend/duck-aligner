package net.walend.duckaligner.duckupdateservice

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
import net.walend.duckaligner.duckupdates.v0.{DuckUpdateService, DuckUpdateServiceGen}
import cats.effect.*
import cats.implicits.toFunctorOps
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.*
import com.comcast.ip4s.*
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore
import smithy4s.http4s.SimpleRestJsonBuilder

object Routes {

  private def duckStoreRoutesF[F[_]: Concurrent]: F[Resource[F, HttpRoutes[F]]] = DucksStateStore.makeDuckStateStore[F].map { ducksStateStore =>
    SimpleRestJsonBuilder.routes(ducksStateStore).resource
  }

  private def docs[F[_]: Sync] =
    smithy4s.http4s.swagger.docs[F](DuckUpdateService)

  def allF[F[_] : Async ]: F[Resource[F, HttpRoutes[F]]] =
    import cats. syntax. all. toSemigroupKOps
    duckStoreRoutesF[F].map(_.map(_ <+> docs))
}

object Main extends IOApp.Simple:

  val run: IO[Nothing] = Routes.allF[IO]
    .flatMap { routes =>
      routes.flatMap { r =>
        EmberServerBuilder
          .default[IO]
          .withPort(port"9000")
          .withHost(host"localhost")
          .withHttpApp(r.orNotFound)
          .build
      }.use(_ => IO.never)
    }