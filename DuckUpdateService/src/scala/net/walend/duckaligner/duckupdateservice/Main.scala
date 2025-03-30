package net.walend.duckaligner.duckupdateservice

import cats.effect.{IO, IOApp}
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{host, ipv4, port}
import cats.effect.*
import cats.implicits.toSemigroupKOps
import net.walend.duckaligner.duckupdateservice.awssdklocation.{AwsLocationRoutes, MapLibreGLRoutes}
import org.http4s.*

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object Main extends IOApp.Simple:

  val run: IO[Nothing] = 
    val routes = DuckUpdateRoutes.allF[IO].map(_.map(
      _.combineK(StaticRoutes.staticFiles)
        .combineK(PingRoutes.pingRoutes)
        .combineK(MapLibreGLRoutes.mapLibreGLProxy)
    ))
    
    routes.flatMap { routes =>
      routes.flatMap { r =>
        EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withHttpApp(r.orNotFound)
          .build
      }.use(_ => IO.never)
    }
