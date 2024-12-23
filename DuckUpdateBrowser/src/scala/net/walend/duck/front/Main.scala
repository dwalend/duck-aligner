package net.walend.duck.front

import calico.IOWebApp
import calico.html.io.label
import cats.effect.{IO, Resource}
import org.scalajs.dom.document
import fs2.dom.HtmlElement
import calico.html.io.forString

object Main extends IOWebApp:
  def render: Resource[IO, HtmlElement[cats.effect.IO]] =
    for {
      client <- DuckUpdateClient.duckUpdateClient
      hi <- label("Hello!")
      geoLocator = GeoLocator.geolocator(document,client)
    } yield {
      println("DuckUpdateClient ducks!")
      geoLocator.geoLocate()

      hi
    }