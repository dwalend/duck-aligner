package net.walend.duck.front

import cats.effect.{ExitCode, IO, IOApp, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint}
import org.http4s.Uri
import org.http4s.client.Client
import org.scalajs.dom.document
import smithy4s.http4s.SimpleRestJsonBuilder

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    println("DuckUpdateClient ducks!")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("DuckUpdateClient, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)

    IO(DuckUpdateClient.geolocate(document)).map(_ => ExitCode.Success)    
