package net.walend.sharelocationservice

//todo clean up imports
import cats.effect.IO
import cats.effect.kernel.Resource
import org.http4s.*
//import org.http4s.implicits.*
import munit.CatsEffectSuite

class ForecastSourceSpec extends CatsEffectSuite:

  val forecastResource: Resource[IO, ForecastSource[IO]] =
    import org.http4s.ember.client.EmberClientBuilder
    for
      client <- EmberClientBuilder.default[IO].build
    yield
      ForecastSource.forecastSource(client)  //todo see if there's some interesting test client

  val coordinates: Coordinates = Coordinates(38.8894,-77.0352)
  val expectedForecast: Forecast = Forecast("cloudy",82)

  test("Call the national weather service URL and get a response") {
    assertIO(forecastResource.use{ weatherSource =>
      weatherSource.get(coordinates).map(println(_))
    }, ())
  }

