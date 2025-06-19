package net.walend.duckaligner.duckupdateservice

import cats.data.Kleisli
import cats.effect.{IO, IOApp}
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.ipv4
import cats.implicits.toSemigroupKOps
import net.walend.duckaligner.duckupdateservice.awssdklocation.MapLibreGLRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.slf4j.Slf4jLogger

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object Main extends IOApp.Simple:

  val run: IO[Nothing] = 
    val routeResourceIO = DuckUpdateRoutes.allF[IO].map(_.map(
      _.combineK(StaticRoutes.staticFiles)
        .combineK(PingRoutes.pingRoutes)
        .combineK(MapLibreGLRoutes.mapLibreGLProxy)
    ))
    val dsl = new Http4sDsl[IO]{}
    import dsl.*

    val mainResource = for{
      log <- Slf4jLogger.create[IO].toResource
      routeResource <- routeResourceIO.toResource
      routes <- routeResource
      emberServer <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withHttpApp(routes.orNotFound)
        .withErrorHandler{(t:Throwable) =>
          log.error(t)("Ember server trap") *> InternalServerError()}
        .build
        .handleErrorWith{ (t:Throwable) =>
          log.error(t)("Top level error trap").toResource
        }
      _ <- log.info("duck update server started").toResource
    } yield emberServer
    mainResource.use(_ => IO.never)

