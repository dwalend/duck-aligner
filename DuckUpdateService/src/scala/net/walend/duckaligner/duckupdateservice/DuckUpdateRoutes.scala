package net.walend.duckaligner.duckupdateservice

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
import net.walend.duckaligner.duckupdates.v0.DuckUpdateServiceGen
import cats.effect.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.*
import com.comcast.ip4s.*
import net.walend.duckaligner.duckupdateservice.store.DucksStateStore
import smithy4s.http4s
import smithy4s.http4s.SimpleRestJsonBuilder

object Routes {

  private val duckStoreRoutesIO: IO[Resource[IO, HttpRoutes[IO]]] = DucksStateStore.makeDuckStateStore[IO].map{ ducksStateStore =>
    SimpleRestJsonBuilder.routes(ducksStateStore).resource
  }

/*
  private val duckStoreRoutes: Resource[IO, HttpRoutes[IO]] = SimpleRestJsonBuilder.routes(DucksStateStore.makeDuckStateStore[IO],IO).resource
*/
//  private val docs = smithy4s.http4s.swagger.docs[IO](DucksStateStore)

  val allIO: IO[Resource[IO, HttpRoutes[IO]]] = duckStoreRoutesIO

/*  
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(DucksStateStore.duckStateStore[IO]).resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
  
 */
}

object Main extends IOApp.Simple:

  val run: IO[Nothing] = Routes.allIO
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