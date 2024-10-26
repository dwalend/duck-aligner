package net.walend.duckaligner.duckupdateservice

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
import net.walend.duckaligner.duckupdates.v0.DuckUpdateServiceGen
import cats.effect.*
import cats.implicits.toFunctorOps
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.*
import com.comcast.ip4s.*
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore
import smithy4s.http4s
import smithy4s.http4s.SimpleRestJsonBuilder

object Routes {

  private def duckStoreRoutesF[F[_]: Concurrent]: F[Resource[F, HttpRoutes[F]]] = DucksStateStore.makeDuckStateStore[F].map { ducksStateStore =>
    SimpleRestJsonBuilder.routes(ducksStateStore).resource
  }

/*
  private val duckStoreRoutes: Resource[IO, HttpRoutes[IO]] = SimpleRestJsonBuilder.routes(DucksStateStore.makeDuckStateStore[IO],IO).resource
*/
//  private val docs = smithy4s.http4s.swagger.docs[IO](DucksStateStore)

  def allF[F[_]: Concurrent]: F[Resource[F, HttpRoutes[F]]] = duckStoreRoutesF[F]

/*  
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(DucksStateStore.duckStateStore[IO]).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
  
 */
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