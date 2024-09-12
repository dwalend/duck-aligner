package net.walend.duck.front

import org.scalajs.dom.{Geolocation, Position, PositionError, document}

object Hello:
  def main(args: Array[String]): Unit =
    println("Hello ducks!")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("Hello, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)
    geolocate()


  def geolocate(): Unit =
    val window = document.defaultView
    val nav = window.navigator
    val geo: Geolocation = nav.geolocation

    def onSuccess(p: Position): Unit =
      println(s"latitude=${p.coords.latitude}") // Latitude
      println(s"longitude=${p.coords.longitude}") // Longitude

    def onError(p: PositionError): Unit = println(s"Error ${p.code} ${p.message}")

    geo.getCurrentPosition(onSuccess _,onError _)