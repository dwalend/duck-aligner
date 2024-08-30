package tutorial.webapp

object Hello {
  def main(args: Array[String]): Unit = {
    println("Hello world!")
  }

  def later[A,B](lazyList:LazyList[A], func:LazyList[A] => LazyList[B]) = {
    lazyList.appendedAll(func(lazyList))
  }

  val fib: LazyList[Int] = later(LazyList(0,1),{ stream => stream.zip(stream.tail).map(p => p._1 + p._2)})
}