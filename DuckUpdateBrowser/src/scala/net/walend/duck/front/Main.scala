package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import org.scalajs.dom.document
import fs2.dom.{HtmlDocument, HtmlElement}

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import typings.maplibreGl.global.maplibregl.Map
import typings.maplibreGl.mod.MapOptions

@JSExportTopLevel("Main")
object Main extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)
  
  def render: Resource[IO, HtmlElement[IO]] =



    import calico.html.io.{*, given}
    val mapStyle = "Standard"; // e.g., Standard, Monochrome, Hybrid, Satellite
    val awsRegion = "us-east-1"; // e.g., us-east-2, us-east-1, us-west-2, etc.

    for
      hi <- label("Hello!")
//      mapDiv <- div("map")
//      client <- DuckUpdateClient.duckUpdateClient
      apiKey = ???
      styleUrl = s"https://maps.geo.$awsRegion.amazonaws.com/v2/styles/$mapStyle/descriptor?key=$apiKey"

//todo ??      doc: HtmlDocument[IO] = window.document
//      geoLocator = GeoLocator.geolocator(document,client)
    yield
//      geoLocator.geoLocate()

      //todo make this a resource
      val map = new Map(new MapOptions {
        style = styleUrl
        var container = "map"
        center = (25.24, 36.31)
        zoom = 2
      })

      println("DuckUpdateClient ducks!")
      hi

/*


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
