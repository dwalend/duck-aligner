package net.walend.duck.front

import calico.*
import calico.html.io.{*, given}
import cats.effect.*
import fs2.*
import fs2.concurrent.*
import fs2.dom.*

case class AddDuckMapButton(window: Window[IO])://addDuckWidget: AddDuckWidget):
  private def openAddDuckButton(
                           ): Resource[IO, HtmlButtonElement[IO]] = {
    button(
      onClick --> (_.foreach { _ =>
        for
          _ <- IO.println("button pushed")
        yield ()
      }),
      "Call Duck via SMS"
    )
  }

  def render: Resource[IO, HtmlElement[IO]] =
    for
      showPopup <- SignallingRef[IO].of(false).toResource
      popup <- div(
        styleAttr := "display: none; position: absolute; z-index: 100; background: white; padding: 10px;",
        // override display based on signal
        styleAttr <-- showPopup.map(shown => if shown then "display: block; ..." else "display: none;"),
        AddDuckWidget(window).render,
        button(
          onClick --> (_.foreach(_ => showPopup.set(false))),
          "Done"
        )
      )
      widget <- div(
        cls := "maplibregl-ctrl maplibregl-ctrl-group",
        button(
          onClick --> (_.foreach(_ => showPopup.set(true))),
          "Call Ducks"
        ),
        popup
      )
    yield widget

