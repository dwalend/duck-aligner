package net.walend.duck.front

import net.walend.duckaligner.duckupdates.v0.DuckInfo

// The "Image" DSL is the easiest way to create images
import doodle.image.*
// Colors and other useful stuff
import doodle.core.*


/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */

object SvgDuck:

  private def duckIcon(duckInfo: DuckInfo): Image =
    Image.circle(16).fillColor(Color.red).noStroke

  def duckSvg(duckInfo: DuckInfo) =
    // Extension methods
    import doodle.image.syntax.all.*
    // Render to a window using Svg
    import doodle.svg.*
    import cats.effect.unsafe.implicits.global
    val frame = Frame(duckInfo.id.toString)

//todo - might smooth out the blinks duckIcon(duckInfo).compile.drawWithFrameToIO(frame)
    duckIcon(duckInfo).drawWithFrame(frame)
