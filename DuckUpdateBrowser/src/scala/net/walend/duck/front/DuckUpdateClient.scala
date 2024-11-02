package net.walend.duck.front

import org.scalajs.dom.html.Document
import org.scalajs.dom.{Geolocation, Position, PositionError}

object DuckUpdateClient:
  def geolocate(document: Document): Unit =
    val window = document.defaultView
    val nav = window.navigator
    val geo: Geolocation = nav.geolocation

    def onSuccess(p: Position): Unit =
      println(s"latitude=${p.coords.latitude}")
      println(s"longitude=${p.coords.longitude}")
      println(s"altitude=${p.coords.altitude}")
      println(s"speed=${p.coords.speed}")
      println(s"heading=${p.coords.heading}")
      println(s"accuracy=${p.coords.accuracy} m")
      println(s"altitudeAccuracy=${p.coords.altitudeAccuracy}")
      println(s"timestamp=${p.timestamp}")

    def onError(p: PositionError): Unit = println(s"Error ${p.code} ${p.message}")

    geo.getCurrentPosition(onSuccess _,onError _)
/*
import org.http4s.ember.client.EmberClientBuilder


object ClientImpl extends IOApp.Simple {

  val helloWorldClient: Resource[IO, HelloWorldService[IO]] = for {
    client <- EmberClientBuilder.default[IO].build
    helloClient <- SimpleRestJsonBuilder(HelloWorldService)
      .client(client)
      .uri(Uri.unsafeFromString("http://localhost:9000"))
      .resource
  } yield helloClient

  val run = helloWorldClient.use(c =>
    c.hello("Sam", Some("New York City"))
      .flatMap(greeting => IO.println(greeting.message))
  )

}
    
*/