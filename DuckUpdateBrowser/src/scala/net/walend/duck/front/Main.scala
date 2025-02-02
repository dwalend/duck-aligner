package net.walend.duck.front

import calico.IOWebApp
import calico.html.io.{forString, label}
import cats.effect.{IO, Resource}
import org.scalajs.dom.document
import fs2.dom.HtmlElement
//import typings.maplibreGl.global.maplibregl.Map
//import typings.maplibreGl.mod.MapOptions

/*
object Main:
  def main(args: Array[String]): Unit =
    println("Hello, world")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("Hello, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)
*/
/*
object Main extends IOWebApp:
  def render: Resource[IO, HtmlElement[IO]] =
    div("Toto, I've a feeling we're not in Kansas anymore.")
*/

object Main extends IOWebApp:
  def render: Resource[IO, HtmlElement[IO]] =
    println("console works")
    for
      client <- DuckUpdateClient.duckUpdateClient
      geoLocator = GeoLocator.geolocator(document,client)
      hi <- label("Hello!")
      _ = println("console in for")
    yield
      geoLocator.geoLocate()
      println("DuckUpdateClient ducks!")
      hi

/*
    val apiKey = "v1.public.eyJqdGkiOiI1YWU4N2FkMS0zNjBiLTRhNDAtOTE0YS1iMTg4MmEyNzkzNmQifZzcWH3kHO3awM0t1TDcBsYC1KmzFBD-ZGjWMnwIZmBXMHqH28DTii47s_SZUWYeNn3jRCC1bZzk0ZH-OsYRtl_Qxtcy20Yam5RFHNw3o0u8ZLEsZDuss_KATAZBv_SFU-0BEZx82ls2XqRJs0O4cBKOEeumx-at105xuWVb6JjqW4O0YIwJeeWIjdvIn6oqosPvD68FtfizZCQ9eDVxjrhBUvepiW-feMPD_hvCjpATxEhDA3Tt-RU6bDJKr70cPyDwGfYlCeGBqGVbtJ4AZ5N2167iKt5mQqy8SOWZfHqiDFoTPUNip_szFEGyw9Jb94HRU5CG5SFbN8_xApQm75Q.ZWU0ZWIzMTktMWRhNi00Mzg0LTllMzYtNzlmMDU3MjRmYTkx"

    val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
    val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.
    val styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"

    println("console")
    label("Hello!")

    for
//      client <- DuckUpdateClient.duckUpdateClient
      hi <- label("Hello!")
      _ = println("console")
//      geoLocator = GeoLocator.geolocator(document,client)
//      mapDiv <- div("map")//MapLibreGL.mapDiv(client)
    yield
      println("DuckUpdateClient ducks!")
      hi
*/
//      geoLocator.geoLocate()
      /*
      val map = new Map(new MapOptions {
        style = styleUrl
        var container = "map1"
        center = (25.24, 36.31)
        zoom = 2
      })
      */
