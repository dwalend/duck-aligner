package net.walend.duckaligner.wooden

import cats.effect.syntax.all.effectResourceOps
import cats.effect.{Async, IO, Resource}
import cats.syntax.all.{catsSyntaxEither, toFlatMapOps}
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.uri
import smithy4s.http4s.SimpleRestJsonBuilder


object DuckUpdateClient:
  val origin = uri"http://localhost:9000"

  def duckUpdateClient: Resource[IO, DuckUpdateService[IO]] = for {
    httpClient: Client[IO] <- EmberClientBuilder.default[IO].build
    duckUpdateClient <- SimpleRestJsonBuilder(DuckUpdateService).client(httpClient).uri(origin).resource
  } yield duckUpdateClient



