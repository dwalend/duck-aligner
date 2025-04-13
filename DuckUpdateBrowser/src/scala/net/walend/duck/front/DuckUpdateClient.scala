package net.walend.duck.front

import cats.effect.{Async, IO, Resource}
import fs2.dom.Window
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.dom.FetchClientBuilder
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.syntax.all.{catsSyntaxEither, toFlatMapOps}
import cats.effect.syntax.all.effectResourceOps


object DuckUpdateClient:
  val stageName = "/dev"
  def duckUpdateClient[F[_]: Async]: Resource[F, DuckUpdateService[F]] = for {
    httpClient: Client[F] <- FetchClientBuilder[F].resource
    origin: Uri <- Window[F].location.origin.flatMap(o => Uri.fromString(o+stageName).liftTo[F]).toResource
    duckUpdateClient <- SimpleRestJsonBuilder(DuckUpdateService).client(httpClient).uri(origin).resource
  } yield duckUpdateClient



