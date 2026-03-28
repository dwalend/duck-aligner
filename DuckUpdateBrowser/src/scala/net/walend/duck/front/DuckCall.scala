package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef
import fs2.dom.{HtmlDivElement, HtmlElement}
//import org.scalajs.dom.html.Document

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
//    val document: Document = org.scalajs.dom.document
    //val duckName = duckNameFromUriQuery(document) //todo send via proposing an event

    for
//      client: DuckUpdateService[IO] <- DuckUpdateClient.duckUpdateClient[IO]
      appDiv <- AddDuckWidget(window).render //callDucks() //
    yield
      println("Call ducks!")
      appDiv
  }

  private def callDucks(): Resource[IO, HtmlDivElement[IO]] =
    import calico.html.io.{*, given}
    import calico.*
    import fs2.*
    import fs2.dom.*
    
    def captureInput(
                      self: HtmlInputElement[IO],
                      sink: String => IO[Unit]
                    ): Pipe[IO, Event[IO], Nothing] =
      _.evalMap(_ => self.value.get).foreach(sink)

    //todo pattern for a duck name or an sms text number
    def inputText(placeholderText:String): Resource[IO, (SignallingRef[IO, String], HtmlInputElement[IO])] =
      for
        text <- SignallingRef[IO].of("").toResource
        htmlInput <- input.withSelf { self =>
          (
            typ := "text",
            placeholder := placeholderText,
            onInput --> captureInput(self, text.set)
          )
        }
      yield (text,htmlInput)

    def sendTextButton(duckNameRef:SignallingRef[IO, String]) = button(
      onClick --> (_.foreach { _ =>
        for
          duckName <- duckNameRef.get
          _ <- window.location.assign(s"sms:+11234567890?body=Hello%20$duckName")
        yield()
      }),
      "Call Duck"
    )

    def startMapButton(duckNameRef:SignallingRef[IO, String]): Resource[IO, HtmlButtonElement[IO]] =
      button(onClick --> (_.foreach { _ =>
        for
          duckName <- duckNameRef.get
          _ <- IO.println("About to load a new file")
          _ <- window.location.assign(s"map.html?duckName=$duckName") //load a new file in the browser
          _ <- IO.println("This might not run") // Seems to happen
        yield ()
      }),
        "Join Ducks"
      )

    for
      duckNameInput <- inputText("Duck Name")
      //todo add an sms number input
      //todo add a message text input with default text "Duck with me"
      component <- div(
        duckNameInput._2,
        sendTextButton(duckNameInput._1),
        div(
          startMapButton(duckNameInput._1)
        )
      )
    yield component

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


