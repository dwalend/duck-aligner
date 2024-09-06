package net.walend.sharelocationservice

import cats.effect.Async
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s.{ipv4, port}
import fs2.io.net.Network
import net.walend.sharelocationservice.log.Logger
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder

object ShareLocationServiceServer:

  def run[F[_]: Async: Network]: F[Nothing] = {
    for {
      jokeClient <- EmberClientBuilder.default[F].build
      jokeAlg = Jokes.impl[F](jokeClient)
      helloWorldAlg = HelloWorld.impl[F]
      //joke and forecast can't share a client!
      forecastClient <- EmberClientBuilder.default[F].build
      forecastSource = ForecastSource.forecastSource[F](forecastClient)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = (
        ShareLocationServiceRoutes.helloWorldRoutes[F](helloWorldAlg) combineK 
        ShareLocationServiceRoutes.jokeRoutes[F](jokeAlg) combineK 
        ShareLocationServiceRoutes.forecastRoutes[F](forecastSource)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <- 
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
