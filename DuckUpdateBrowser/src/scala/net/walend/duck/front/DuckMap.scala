package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement, Window}
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri
import org.scalajs.dom.html.Document

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

case class DuckMap(divId:String, addDuck:HtmlElement[IO]):

  def render(window: Window[IO]): Resource[IO, HtmlElement[IO]] =
    for
      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      eventStore <- EventStore.create[IO]()
      document: Document = org.scalajs.dom.document
      geoIO = GeoIO(document)
      duckName = duckNameFromUriQuery(document) //todo send via proposing an event
      duckId <- eventStore.sendDuckInfo(duckName, client).toResource
      duckMapUpdater <- DuckMapUpdater(client, eventStore, document, divId, geoIO, duckId, addDuck).startUpdates()
      appDiv <- frontUI(window)
    yield
      println("See ducks!")
      appDiv

  private def frontUI(window: Window[IO]): Resource[IO, HtmlElement[IO]] =
    window.document.getElementById(divId).map(_.get).map(_.asInstanceOf[HtmlElement[IO]]).toResource

  private def duckNameFromUriQuery(document: org.scalajs.dom.html.Document): String =
    val uri = Uri.unsafeFromString(document.documentURI)
    uri.query.pairs.toMap.apply("duckName").get
    
    