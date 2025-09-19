import doodle.core.*
import doodle.image.*
import doodle.image.syntax.all.*
import doodle.image.syntax.core.*
import doodle.java2d.repl.*
import doodle.java2d.java2dRenderer
import cats.effect.unsafe.implicits.global

val duckBody = Image
  .circle(100)
  .scale(1,0.5)
  .fillColor(Color.lightGoldenrodYellow)
  .strokeColor(Color.orange)

val duckHead = Image
  .circle(50)
  .scale(1,0.5)
  .fillColor(Color.lightGoldenrodYellow)
  .strokeColor(Color.orange)

val duckNeck = Image
  .rectangle(20,30)
  .fillColor(Color.lightGoldenrodYellow)
  .strokeColor(Color.orange)

val duckBill = Image
  .triangle(25,10)
  .fillColor(Color.lightGoldenrodYellow)
  .strokeColor(Color.orange)


val duck = duckHead
  .above(duckNeck)
  .above(duckBody)


duck.draw()

