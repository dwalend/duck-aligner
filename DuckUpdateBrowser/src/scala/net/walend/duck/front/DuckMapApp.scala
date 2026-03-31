package net.walend.duck.front

import calico.IOWebApp
import calico.html.io.{*, given}
import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeError
import fs2.dom.{HtmlDivElement, HtmlElement}
import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("DuckMapApp")
object DuckMapApp extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)
    println("end atlMain()")

  def render: Resource[IO, HtmlElement[IO]] =
    (for
      _ <- Resource.make(IO.println("IOWebApp render START"))(_ => IO.println("IOWebApp render FINALIZED"))
//      addDuck <- AddDuckWidget(window).render
      addDuckButton <- AddDuckMapButton(window).render
//      duckMap <- DuckMap("app", addDuck).render(window)
      duckMap <- DuckMap("app", addDuckButton).render(window)
      appDiv <- div("calico") //just enough to keep IOWebApp going
    yield appDiv)
      .onError(e => Resource.eval(IO.println(s"render FAILED: ${e.getMessage}\n${e.getStackTrace.mkString("\n")}")))
