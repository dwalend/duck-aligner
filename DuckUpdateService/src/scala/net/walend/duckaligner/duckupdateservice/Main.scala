package net.walend.duckaligner.duckupdateservice

import cats.effect.{IO, IOApp}
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{host, port}
import cats.effect.*
import cats.implicits.toSemigroupKOps
import org.http4s.*

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object Main extends IOApp.Simple:

  val run: IO[Nothing] = 
    val routes = DuckUpdateRoutes.allF[IO].map(_.map(_.combineK(StaticRoutes.staticFiles)))
    
    routes.flatMap { routes =>
      routes.flatMap { r =>
        EmberServerBuilder
          .default[IO]
          .withPort(port"9000")
          .withHost(host"localhost")
          .withHttpApp(r.orNotFound)
          .build
      }.use(_ => IO.never)
    }
