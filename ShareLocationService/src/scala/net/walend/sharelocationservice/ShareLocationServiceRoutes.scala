package net.walend.sharelocationservice

import cats.{Monad, MonadThrow}
import cats.effect.Sync
import cats.syntax.all.*
import fs2.io.file.Files
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl

object ShareLocationServiceRoutes:

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }

  def forecastRoutes[F[_]: Sync](forecastSource: ForecastSource[F]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "forecast" / Coordinates(coordinates) =>
        for {
          forecast: Forecast <- forecastSource.get(coordinates)
          response <- Ok(forecast.toResponseString)
        } yield response
    }
  
  def staticFiles[F[_]: MonadThrow : Files]:HttpRoutes[F] =
    import fs2.io.file.Path
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case request@GET -> Root / "static" / "hello.html" =>
        StaticFile.fromPath(Path("/Users/dwalend/projects/duck-aligner/FrontEnd/hello.html"), Some(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
      case request@GET -> Root / "static" / "FrontEnd.js" =>
        StaticFile.fromPath(Path("/Users/dwalend/projects/duck-aligner/.bleep/builds/normal/.bloop/FrontEnd/FrontEnd.js"), Option(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
    }