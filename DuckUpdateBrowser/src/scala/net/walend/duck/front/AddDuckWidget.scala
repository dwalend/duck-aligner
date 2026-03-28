package net.walend.duck.front

import calico.*
import calico.html.io.{*, given}
import cats.effect.*
import fs2.*
import fs2.concurrent.*
import fs2.dom.*
import org.http4s.Uri
import org.http4s.Uri.Path

case class AddDuckWidget(window: Window[IO]):
  def render: Resource[IO, HtmlElement[IO]] = {

    for
      duckNameInput <- inputTextBox("Duck Name")
      smsTextNumber <- inputTextBox("Sms Number")
      duckMessageInput <- inputTextBox("Duck with me") //duck message - eventually a text spinner
      //todo add an sms number input
      //todo add a message text input with default text "Duck with me"
      widget <- div(
        duckNameInput._1,
        smsTextNumber._1,
        duckMessageInput._1,
        sendSmsButton(duckNameInput._2,smsTextNumber._2,duckMessageInput._2)
      )
    yield widget
  }

  private def sendSmsButton(
                             duckNameRef: SignallingRef[IO, String],
                             smsTextNumberRef: SignallingRef[IO, String],
                             duckMessageRef: SignallingRef[IO, String],
                           ): Resource[IO, HtmlButtonElement[IO]] = {
    def linkToMap(uriToCurrent:Uri,duckName:String):String = {
      val path = Path(uriToCurrent.path.segments.dropRight(1),true,false) / "map.html"
      val uri = uriToCurrent.withPath(path).removeQueryParam("duckName").withQueryParam("duckName",duckName)

      uri.toString
    }

    button(
      onClick --> (_.foreach { _ =>
        for
          duckName <- duckNameRef.get
          smsTextNumber <- smsTextNumberRef.get
          duckMessage <- duckMessageRef.get
          hrefRef = window.location.href
          href <- hrefRef.get.map(Uri.unsafeFromString)
          duckMapLink = linkToMap(href,duckName)
          message = s"""$duckMessage,%20$duckName!%20$duckMapLink""" //todo spaces to html link
          smsLink = s"sms:+1$smsTextNumber?body=$message" //s"sms:+11234567890?body=Hello%20$duckName"
          _ <- IO.println(s"got $duckName $duckNameRef")
          _ <- window.location.assign(smsLink)
        yield ()
      }),
      "Call Duck"
    )
  }

  //todo pattern for a duck name or an sms text number
//todo maybe this is a case class
  private def inputTextBox(placeholderText: String): Resource[IO, (HtmlInputElement[IO], SignallingRef[IO, String])] =
    for
      textRef <- SignallingRef[IO].of("No Duck").toResource
      htmlInput <- input.withSelf { self =>
        (
          typ := "text",
          placeholder := placeholderText,
          onInput --> captureInput(self, textRef.set)
        )
      }
    yield (htmlInput,textRef)

  def captureInput(
                    self: HtmlInputElement[IO],
                    sink: String => IO[Unit]
                  ): Pipe[IO, Event[IO], Nothing] =
    _.evalMap(_ => self.value.get).foreach(sink)


/*
def emptyAppDiv: IO[Unit] =
  IO(document.getElementById("app")).map {
    case el: org.scalajs.dom.HTMLDivElement => el.innerHTML = ""
    case _ => // handle case where element is not found
  }
*/


/*
import calico.html.io.*
import calico.syntax.*
import cats.effect.*
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
 */
// Minimal facade for MapLibre IControl
/*
@js.native
trait IControl extends js.Object {
  def onAdd(map: js.Any): dom.HTMLElement
  def onRemove(map: js.Any): Unit
}

class AddDuckButtonControl(onClick: IO[Unit]) extends IControl {
  private var container: dom.HTMLElement = _

  override def onAdd(map: js.Any): dom.HTMLElement = {
    // Create the button UI using Calico
    // We use .unsafeRunSync here only because MapLibre expects a synchronous HTMLElement return
    val element = div(
      cls := "maplibregl-ctrl maplibregl-ctrl-group",
      button(
        typ := "button",
        "🚀",
//        onClickEvents --> (_.foreach(_ => onClick))
      )
    ).renderInto(dom.document.createElement("div")).unsafeRunSync()

    container = element
    element
  }

  override def onRemove(map: js.Any): Unit = {
    if (container != null && container.parentNode != null) {
      container.parentNode.removeChild(container)
    }
  }
}

// Assuming 'map' is your MapLibre map instance
val myButton = new AddDuckButtonControl(IO.println("Button clicked via Calico!"))

// Add to the top-right corner
map.addControl(myButton.asInstanceOf[js.Any], "top-right")

 */

