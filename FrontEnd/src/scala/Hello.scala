package tutorial.webapp

import org.scalajs.dom.document

object Hello {
  def main(args: Array[String]): Unit = {
    println("Hello world!")
    val parNode = document.createElement("p")
    val textNode = document.createTextNode("Hello, world")
    parNode.appendChild(textNode)
    document.body.appendChild(parNode)
  }

  def later[A,B](lazyList:LazyList[A], func:LazyList[A] => LazyList[B]): LazyList[A|B] = {
    lazyList.appendedAll(func(lazyList))
  }

  val fib: LazyList[Int] = later(LazyList(0,1),{ stream => stream.zip(stream.tail).map(p => p._1 + p._2)})
}