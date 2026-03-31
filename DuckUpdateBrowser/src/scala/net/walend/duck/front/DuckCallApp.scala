package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.dom.HtmlElement

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("DuckCallApp")
object DuckCallApp extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)

  def render: Resource[IO, HtmlElement[IO]] = {

    for
//      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      appDiv <- AddDuckWidget(window).render //callDucks() //
    yield
      println("Call ducks!")
      appDiv
  }


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
/*
  private def duckNameFromUriQuery(document:org.scalajs.dom.html.Document):String =
    val uri = Uri.unsafeFromString(document.documentURI)
    uri.query.pairs.toMap.apply("duckName").get

 */

/*   //todo how to do style
      div(
        idAttr := "map",
        styleAttr := "min-height: 90%;",
        "map"
      ),

 */


/*
def emptyAppDiv: IO[Unit] =
  IO(document.getElementById("app")).map {
    case el: org.scalajs.dom.HTMLDivElement => el.innerHTML = ""
    case _ => // handle case where element is not found
  }
*/


