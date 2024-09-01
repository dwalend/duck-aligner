package net.walend.sharelocationservice

//todo clean up these imports
import cats.effect.Concurrent
import cats.syntax.all.*
//import io.circe.{Encoder, Decoder}
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
//import org.http4s.circe.*
import org.http4s.Method.GET

/**
 * Source of weather forecasts from The National Weather Service API Web Service https://www.weather.gov/documentation/services-web-api
 *
 * To get from lat,lon to a short forecast is two steps. First touch a url like https://api.weather.gov/points/38.8894,-77.0352 to get the URL for the forecast - which will look like https://api.weather.gov/gridpoints/LWX/97,71/forecast . Touch that to find the "shortForecast" and "temperature"
 *
 */

trait WeatherSource[F[_]]:
  /**
   * @param coordinates
   * @return the weather
   */
  def get(coordinates:Coordinates):F[Weather]

object WeatherSource:
  def apply[F[_]](using ev:WeatherSource[F]): WeatherSource[F] = ev

  def weatherSource[F[_]: Concurrent](client: Client[F]):WeatherSource[F] = new WeatherSource[F]:
    val dsl = new Http4sClientDsl[F]{}
    import dsl.*

    override def get(coordinates: Coordinates): F[Weather] =
      //For the coordinates look up the right PointResponse
      val pointsRequest: Request[F] = GET(uri"https://api.weather.gov/points" / coordinates.forUrl)

      val pointsResponse: F[String] = client.expect[String](pointsRequest).adaptError{ case t:Throwable => WeatherError(coordinates,t)} //todo move error handling to the end

      pointsResponse.map { pr => println(pr) }.map { _ => Weather("toads", 42) }
      //Use the URL from the PointResponse to look up the forecast
      //And produce the weather


    case class PointResponse(forecastUrl: Uri)

    object PointResponse:
      def apply(jsonString: String): PointResponse =
        ??? //receive a string of json to create a point response

case class Weather(shortForecast:String,temperature:Int):
  def temperatureWord =
    ??? //convert the given temperature value into a terse string

object Weather:
  def apply(jsonString: String): Weather =
    ??? //receive a string of json to create a Weather

case class Coordinates(lat:Double,lon:Double):
  def forUrl:String = s"$lat,$lon"

case class WeatherError(coordinates: Coordinates,e: Throwable) extends RuntimeException