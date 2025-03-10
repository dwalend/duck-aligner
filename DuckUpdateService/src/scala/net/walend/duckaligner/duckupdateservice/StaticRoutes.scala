package net.walend.duckaligner.duckupdateservice

import cats.MonadThrow
import cats.effect.Sync
import fs2.io.file.Files
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl

/**
 * Serve static files
 *
 * @author David Walend
 * @since v0.0.0
 */
object StaticRoutes {
  def staticFiles[F[_]: MonadThrow : Files : Sync]:HttpRoutes[F] =
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      case request@GET -> Root / "static" / "hello.html" =>
        StaticFile.fromResource("hello.html", Option(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
      case request@GET -> Root / "static" / "main.js" =>
        //./.bleep/builds/normal/.bloop/DuckUpdateBrowser/
        StaticFile.fromResource("main.js", Option(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
      case request@GET -> Root / "static" / "style.css" =>
        StaticFile.fromResource("style.css", Option(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
    }
}
