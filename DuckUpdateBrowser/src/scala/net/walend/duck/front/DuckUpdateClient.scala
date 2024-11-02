package net.walend.duck.front

import cats.effect.{IO, IOApp, Resource}
import net.walend.duckaligner.duckupdates.v0.{DuckId, DuckUpdate, DuckUpdateService, GeoPoint, UpdatePositionOutput}
import org.http4s.Uri
import org.http4s.ember.client.EmberClientBuilder
import org.scalajs.dom.html.Document
import org.scalajs.dom.{Geolocation, Position, PositionError}
import smithy4s.http4s.SimpleRestJsonBuilder

object DuckUpdateClient:

  def geolocate(document: Document): Unit =
    val window = document.defaultView
    val nav = window.navigator
    val geo: Geolocation = nav.geolocation

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

      import cats.effect.unsafe.implicits.global

      IO.println("test IO.println again").unsafeRunAsync{
        case Right(v) => println(s"still working")
        case Left(t) => println(s"errored with ${t.getMessage}")
      }
    /*
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
      println(s"before duckUpdateClient with $duckUpdate")


      println(s"before duckUpdateClient")
      val duckUpdateClient: Resource[IO, DuckUpdateService[IO]] = for {
        client <- EmberClientBuilder.default[IO].build
        duckUpdateClient <- SimpleRestJsonBuilder(DuckUpdateService)
          .client(client)
          .uri(Uri.unsafeFromString("http://localhost:9000"))
          .resource
      } yield duckUpdateClient
      println(s"duckUpdateClient is $duckUpdateClient")

      duckUpdateClient.use(c =>
        println(s"started use with $c")
        c.updatePosition(duckUpdate).flatMap{duckLine =>
          println(s"got duckLine in IO $duckLine")
          IO(duckLine)
        }
      ).unsafeRunAsync{
        case Right(v) => println(s"got duckLine $v")
        case Left(t) => println(s"duckLine errored with ${t.getMessage}")
      }

      println("after duckLine")
      */
    def onError(p: PositionError): Unit = println(s"Error ${p.code} ${p.message}")

    geo.getCurrentPosition(onSuccess _,onError _)


