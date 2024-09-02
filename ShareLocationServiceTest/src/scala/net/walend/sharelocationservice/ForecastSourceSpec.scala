package net.walend.sharelocationservice

//todo clean up imports
import cats.effect.IO
import cats.effect.kernel.Resource
import org.http4s.*
import munit.CatsEffectSuite

class ForecastSourceSpec extends CatsEffectSuite:

  val forecastResource: Resource[IO, ForecastSource[IO]] =
    import org.http4s.ember.client.EmberClientBuilder
    for
      client <- EmberClientBuilder.default[IO].build
    yield
      ForecastSource.forecastSource(client)  //todo see if there's some interesting test client

  val coordinates: Coordinates = Coordinates(38.8894,-77.0352)
  val newtonCoordinates = Coordinates(42.338032,-71.211578) //todo 301 with more than 4 spots after the decimal
  val expectedForecast: Forecast = Forecast("cloudy",82)

  test("Call the national weather service URL and get a response") {
    assertIO(forecastResource.use{ weatherSource =>
      weatherSource.get(coordinates).map(println(_))
    }, ())
  }

  test("Convert a forecast to a fine string") {
    val forecast = Forecast("Toads",55)
    assertEquals(forecast.toResponseString,"Moderate and Toads")
  }
