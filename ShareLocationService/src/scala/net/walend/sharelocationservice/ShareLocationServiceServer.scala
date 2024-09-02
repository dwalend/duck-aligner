package net.walend.sharelocationservice

import cats.effect.Async
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.io.net.Network
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger

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
        ShareLocationServiceRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        ShareLocationServiceRoutes.jokeRoutes[F](jokeAlg) <+>
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
