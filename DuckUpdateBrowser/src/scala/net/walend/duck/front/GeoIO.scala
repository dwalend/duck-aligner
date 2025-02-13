package net.walend.duck.front

import org.scalajs.dom.html.Document
import org.scalajs.dom.{Position, PositionError}
import cats.effect.{IO, Resource}

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
case class GeoIO(document: Document):
  private val geolocation = document.defaultView.navigator.geolocation

  def positionResource(): Resource[IO, Position] = IO.async_ { (cb: Either[Throwable, Position] => Unit) =>
    geolocation.getCurrentPosition(
      p => cb(Right(p)), 
      pe => cb(Left(new RuntimeException(pe.toString))) //todo better exception
    )
  }.toResource
