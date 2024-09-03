package net.walend.sharelocationservice

import cats.effect.IO
import cats.effect.kernel.Resource
import munit.CatsEffectSuite

class ForecastSourceSpec extends CatsEffectSuite:

  val forecastResource: Resource[IO, ForecastSource[IO]] =
    import org.http4s.ember.client.EmberClientBuilder
    for
      client <- EmberClientBuilder.default[IO].build
    yield
      ForecastSource.forecastSource(client)

  val coordinates: Coordinates = Coordinates(38.8894,-77.0352)
  val newtonCoordinates: Coordinates = Coordinates(42.338032,-71.211578)
  val fortyCoordinates: Coordinates = Coordinates(40,-71.211578)

  val expectedForecast: Forecast = Forecast("cloudy",82)

  test("Call the national weather service URL and get a response") {
    assertIO(forecastResource.use{ weatherSource =>
      weatherSource.get(coordinates).map(println(_))
    }, ())
  }

  test("Call the national weather service URL for coordinates that are not to the NWS spec and get a response") {
    assertIO(forecastResource.use { weatherSource =>
      weatherSource.get(newtonCoordinates).map(println(_))
    }, ())
  }

  test("Convert a forecast to a string") {
    val forecast = Forecast("Toads",55)
    assertEquals(forecast.toResponseString,"Moderate and Toads")
  }