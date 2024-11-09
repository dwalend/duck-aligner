package net.walend.duck.front

import cats.effect.IO
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}
import org.scalajs.dom.html.Document
import org.scalajs.dom.{Position, PositionError}

trait GeoLocator:
  def geoLocate():Unit

object GeoLocator:
  def geolocator(document: Document, duckUpdateClient: DuckUpdateService[IO]): GeoLocator =
    new GeoLocator:
      private val geolocation = document.defaultView.navigator.geolocation

      def geoLocate(): Unit = geolocation.getCurrentPosition(onSuccess _, onError _)

      def onSuccess(p: Position): Unit =
        import cats.effect.unsafe.implicits.global
        IO.println("test IO.println in onSuccess").unsafeRunAsync {
          case Right(_) => println(s"Worked!")
          case Left(t) => println(s"errored with ${t.getMessage}")
        }

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

        val duckUpdate: DuckUpdate = DuckUpdate(
          id = DuckId(0),
          snapshot = 0,
          position = geoPoint
        )

        duckUpdateClient.updatePosition(duckUpdate).unsafeRunAsync {
          case Right(v) => println(s"got duckLine $v")
          case Left(t) => println(s"duckLine error ${t.getMessage}")
        }

      def onError(p: PositionError): Unit = println(s"Error ${p.code} ${p.message}")
