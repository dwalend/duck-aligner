package net.walend.duck.front

import org.scalajs.dom.html.Document
import org.scalajs.dom.{Position, PositionError}
import cats.effect.{IO, Resource}
import net.walend.duckaligner.duckupdates.v0.GeoPoint

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
case class GeoIO(document: Document):
  private val geolocation = document.defaultView.navigator.geolocation

  def positionResource(): Resource[IO, GeoPoint] = position().toResource

  def position(): IO[GeoPoint] = IO.async_ { (cb: Either[Throwable, GeoPoint] => Unit) =>
    geolocation.getCurrentPosition(
      p => cb(Right(p.toGeoPoint)),
      pe => cb(Left(new RuntimeException(pe.toString))) //todo better exception
    )
  }

  extension (position: Position)
    private def toGeoPoint: GeoPoint =
      GeoPoint(
        latitude = position.coords.latitude,
        longitude = position.coords.longitude,
        timestamp = position.timestamp.toLong
      )