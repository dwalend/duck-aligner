package net.walend.duck.front

import org.scalajs.dom.document

object Main:
  def main(args: Array[String]): Unit =
    println("DuckUpdateClient ducks!")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("DuckUpdateClient, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)
    DuckUpdateClient.geolocate(document)
