package net.walend.duckaligner.duckupdateservice

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
import net.walend.duckaligner.duckupdates.v0.{DuckUpdateService, DuckUpdateServiceGen}
import cats.effect.*
import cats.implicits.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.*
import com.comcast.ip4s.*
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore
import smithy4s.http4s
import smithy4s.http4s.SimpleRestJsonBuilder

object Routes {
  
  private val duckStoreRoutes: Resource[IO, HttpRoutes[IO]] = SimpleRestJsonBuilder.routes(DucksStateStore).resource
  
//  private val docs = smithy4s.http4s.swagger.docs[IO](DucksStateStore)

  val all: Resource[IO, HttpRoutes[IO]] = duckStoreRoutes

/*  
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(DucksStateStore.duckStateStore[IO]).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
  
 */
}

object Main extends IOApp.Simple:

  val run: IO[Nothing] = Routes.all
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }
    .use(_ => IO.never)

