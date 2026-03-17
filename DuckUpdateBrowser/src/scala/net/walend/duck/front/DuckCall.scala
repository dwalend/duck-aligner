package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement}
import org.http4s.Uri
import org.scalajs.dom.html.Document

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("DuckCall")
object DuckCall extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)

  def render: Resource[IO, HtmlElement[IO]] = {
    val document: Document = org.scalajs.dom.document
    val duckName = duckNameFromUriQuery(document) //todo send via proposing an event

    for
//      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      appDiv <- callDucks()
    yield
      println("See ducks!")
      appDiv
  }

  private def callDucks(): Resource[IO, HtmlDivElement[IO]] =
    import calico.html.io.{*, given}
    import calico.syntax.*

    /*
    def emptyAppDiv: IO[Unit] =
      IO(document.getElementById("app")).map {
        case el: org.scalajs.dom.HTMLDivElement => el.innerHTML = ""
        case _ => // handle case where element is not found
      }
    */

    val startMapButton = button(onClick --> (_.foreach { _ =>
      for {
        _ <- IO.println("Starting navigation...")
        _ <- window.location.assign("map.html?duckName=David") //last switch the view to a server
        _ <- IO.println("This might never run!") // Risky!
      } yield ()
    }), "Duck Call")


    div(
/*   //todo how to do style
      div(
        idAttr := "map",
        styleAttr := "min-height: 90%;",
        "map"
      ),

 */
      startMapButton
    )
    /*
    //todo fill in an app with behavior
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