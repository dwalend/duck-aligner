package net.walend.duck.front

import org.scalajs.dom.document

object Hello {
  def main(args: Array[String]): Unit = {
    println("Hello ducks!")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("Hello, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)
  }
}