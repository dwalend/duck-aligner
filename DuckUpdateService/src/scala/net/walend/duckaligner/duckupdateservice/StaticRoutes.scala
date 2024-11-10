package net.walend.duckaligner.duckupdateservice

import cats.MonadThrow
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
  def staticFiles[F[_]: MonadThrow : Files]:HttpRoutes[F] =
    import fs2.io.file.Path
    val dsl = new Http4sDsl[F]{}
    import dsl.*
    HttpRoutes.of[F] {
      //todo so wrong. Needs a rework via bleep
      case request@GET -> Root / "static" / "hello.html" =>
        StaticFile.fromPath(Path("/Users/dwalend/projects/duck-aligner/DuckUpdateBrowser/hello.html"), Option(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
      case request@GET -> Root / "static" / "DuckUpdateBrowser.js" =>
        StaticFile.fromPath(Path("/Users/dwalend/projects/duck-aligner/.bleep/builds/normal/.bloop/DuckUpdateBrowser/DuckUpdateBrowser.js"), Option(request))
          .getOrElseF(NotFound()) // In case the file doesn't exist
    }
}
