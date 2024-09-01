package net.walend.sharelocationservice

//todo clean up imports
import cats.effect.IO
import org.http4s.*
//import org.http4s.implicits.*
import munit.CatsEffectSuite

class WeatherSourceSpec extends CatsEffectSuite:

  val weatherResource =
    import org.http4s.ember.client.EmberClientBuilder
    for
      client <- EmberClientBuilder.default[IO].build
    yield
      WeatherSource.weatherSource(client)

  val cooridates = Coordinates(38.8894,-77.0352)
  val expectedWeather = Weather("cloudy",82)

  //todo there's something hidden of interest here " missing argument list for value of type (=> Any) => Unit"
  test("Call the national weather service URL and get a response") {
    assertIO(weatherResource.use{ weatherSource =>
      weatherSource.get(cooridates)
    }, expectedWeather)
  }

