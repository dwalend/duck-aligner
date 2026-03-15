package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement}
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Main")
object Main extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)
  
  def render: Resource[IO, HtmlElement[IO]] =
    //todo try delaying creating the map - do it from a UI widget - todo then add the div("map") here
    for
      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      eventStore <- EventStore.create[IO]()
      document = window.document.asInstanceOf[org.scalajs.dom.html.Document] //todo should not need to cast
      geoIO = GeoIO(document)
      duckName = duckNameFromUriQuery(document) //todo send via proposing an event
      duckId <- eventStore.sendDuckInfo(duckName,client).toResource
      duckMapUpdater = DuckMapUpdater(client,eventStore,duckId)
      appDiv <- callDucks(duckMapUpdater)//div("") //todo eventually make this a control overlay
    yield
      println("See ducks!")
      appDiv
  
  private def callDucks(duckMapUpdater:DuckMapUpdater): Resource[IO, HtmlDivElement[IO]] =
    import calico.html.io.{*, given}
    import calico.syntax.*

    val startMapButton = button(onClick(duckMapUpdater.updateForever()), "Click me")

    div(
/*
      div(
        idAttr := "map",
        styleAttr := "min-height: 90%;",
        "map"
      ),

 */
      startMapButton
    )
    /*
    SignallingRef[IO].of("world").toResource.flatMap { name =>
      div(
        label("Your name: "),
        input.withSelf { self =>
          (
            placeholder := "Enter your name here",
            // here, input events are run through the given Pipe
            // this starts background fibers within the lifecycle of the <input> element
            onInput --> (_.foreach(_ => self.value.get.flatMap(name.set)))
          )
        },
        span(" Hello, ", name.map(_.toUpperCase))
      )
    } */

  private def duckNameFromUriQuery(document:org.scalajs.dom.html.Document):String =
    val uri = Uri.unsafeFromString(document.documentURI)
    uri.query.pairs.toMap.apply("duckName").get