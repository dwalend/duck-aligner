package net.walend.duck.front

import org.scalajs.dom.html.Document
import org.scalajs.dom.Position
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
    geolocation.getCurrentPosition(  //todo throws 
      p => cb(Right(p.toGeoPoint)),
      pe => cb(Left(new RuntimeException(pe.toString))) //todo better exception
    )
  }
  /*
          println(s"latitude=${p.coords.latitude}")
          println(s"longitude=${p.coords.longitude}")
          println(s"altitude=${p.coords.altitude}")
          println(s"speed=${p.coords.speed}")
          println(s"heading=${p.coords.heading}")
          println(s"accuracy=${p.coords.accuracy} m")
          println(s"altitudeAccuracy=${p.coords.altitudeAccuracy}")
          println(s"timestamp=${p.timestamp}")
  
          val geoPoint: GeoPoint = GeoPoint(
            latitude = p.coords.latitude,
            longitude = p.coords.longitude,
            timestamp = p.timestamp.toLong
          )
  */

  extension (position: Position)
    private def toGeoPoint: GeoPoint =
      GeoPoint(
        latitude = position.coords.latitude,
        longitude = position.coords.longitude,
        timestamp = position.timestamp.toLong
      )