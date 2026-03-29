package net.walend.duck.front

import calico.IOWebApp
import cats.effect.{IO, Resource}
import fs2.dom.{HtmlDivElement, HtmlElement}
import net.walend.duckaligner.duckupdates.v0.DuckUpdateService
import org.http4s.Uri
import org.scalajs.dom.html.Document

import scala.annotation.unused
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("DuckMapApp")
object DuckMapApp extends IOWebApp:
  @JSExport("main")
  @unused
  def altMain(): Unit =
    println("in atlMain()")
    main(Array.empty)

  def render: Resource[IO, HtmlElement[IO]] =
    DuckMap("app").render(window)