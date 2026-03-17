package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement}
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri
import org.scalajs.dom.html.Document

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
    for
      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      eventStore <- EventStore.create[IO]()
      document: Document = org.scalajs.dom.document
      geoIO = GeoIO(document)
      duckName = duckNameFromUriQuery(document) //todo send via proposing an event
      duckId <- eventStore.sendDuckInfo(duckName,client).toResource
      duckMapUpdater <- DuckMapUpdater(client,eventStore,document,geoIO,duckId).startUpdates()
      appDiv <- frontUI()
    yield
      println("See ducks!")
      appDiv

  private def frontUI(): Resource[IO, HtmlDivElement[IO]] =
    import calico.html.io.{*, given}
    div("")

  private def duckNameFromUriQuery(document:org.scalajs.dom.html.Document):String =
    val uri = Uri.unsafeFromString(document.documentURI)
    uri.query.pairs.toMap.apply("duckName").get