package net.walend.duck.front

import cats.effect.IO
import net.walend.duckaligner.duckupdates.v0.DuckInfo
import doodle.image.Image
import doodle.core.Color
import doodle.core.font.{Font, FontSize}


/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */

object SvgDuck:

  def duckSvg(duckInfo: DuckInfo, age: Long): IO[Unit] =
    import doodle.svg.*
    import doodle.syntax.all.*
    val frame = Frame(duckInfo.id.toString)

    duckIcon(duckInfo, age).compile.drawWithFrameToIO(frame)


  private def duckIcon(duckInfo: DuckInfo, age: Long): Image = {
    val font = Font.defaultSansSerif.copy(size = FontSize.points(8))
    val duck = Image.circle(16).fillColor(Color.red).noStroke
    val label = Image.text(duckInfo.duckName).font(font).fillColor(Color.black).noStroke
    val timer = Image.text(s"${age}s").font(font).fillColor(Color.black).noStroke

    duck.beside(timer).above(label)
  }
