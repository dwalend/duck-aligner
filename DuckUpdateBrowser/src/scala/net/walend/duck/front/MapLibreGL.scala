package net.walend.duck.front
  /*
import calico.html.io.div
import calico.html.io.forString
import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.dom.HtmlDivElement
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService

object MapLibreGL:
  def mapDiv(duckUpdateClient: DuckUpdateService[IO]): Resource[IO, HtmlDivElement[IO]] =
    //todo maybe get the URL from the server instead of just the apiKey
    // todo feed it a duck update with the initial bounds if that makes sense - what should the bounds be for just one duck?
    duckUpdateClient.mapLibreGlKey().flatMap{ apiKey =>
      val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
      val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.
      val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"

      //todo figure out how to make the map in scala js from here

      IO(styleUrl)
    }
    //todo return a thing that has both a div and a thing that can take a duckUpdate to update the map. That thing might need to go into the callback. Chicken-vs-egg problem
    div("map")

   */

