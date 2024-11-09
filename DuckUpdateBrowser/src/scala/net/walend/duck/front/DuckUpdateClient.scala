package net.walend.duck.front

import cats.effect.{IO, Resource}
import fs2.dom.Window
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.syntax.all.catsSyntaxEither


object DuckUpdateClient:
  def duckUpdateClient: Resource[IO, DuckUpdateService[IO]] = for {
    httpClient: Client[IO] <- FetchClientBuilder[IO].resource
    origin: Uri <- Window[IO].location.origin.flatMap(Uri.fromString(_).liftTo[IO]).toResource
    duckUpdateClient <- SimpleRestJsonBuilder(DuckUpdateService).client(httpClient).uri(origin).resource
  } yield duckUpdateClient



