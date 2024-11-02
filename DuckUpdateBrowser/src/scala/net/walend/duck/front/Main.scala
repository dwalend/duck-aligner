package net.walend.duck.front

import cats.effect.{IO, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint}
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.scalajs.dom.document
import smithy4s.http4s.SimpleRestJsonBuilder

object Main:
  def main(args: Array[String]): Unit =
    println("DuckUpdateClient ducks!")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("DuckUpdateClient, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)

    import cats.effect.unsafe.implicits.global
    IO.println("test IO.println in main").unsafeRunAsync {
      case Right(_) => println(s"Worked!")
      case Left(t) => println(s"errored with ${t.getMessage}")
    }
/* todo start here - get a ping client to work
    def ping(client: Client[IO]): IO[Unit] =
      client
        .expect[String]("http://localhost:9000/ping")
        .flatMap(IO.println)

    val run: IO[Unit] = EmberClientBuilder
      .default[IO]
      .build
      .use(client => ping(client))

    run.unsafeRunAsync {
      case Right(_) => println(s"Pinged!")
      case Left(t) => println(s"ping error ${t.getMessage}")
    }
*/
    DuckUpdateClient.geolocate(document)
