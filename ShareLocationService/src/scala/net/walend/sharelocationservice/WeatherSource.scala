package net.walend.sharelocationservice

//todo clean up these imports
import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.{Decoder, DecodingFailure}
import org.http4s.implicits.uri
import org.http4s.{Request, Uri}
//import io.circe.{Encoder, Decoder}
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

      for {
        pointsResponseString: String <- client.expect[String](pointsRequest).adaptError{ case t => WeatherError(coordinates,t)}
        pointResponse:PointResponse = PointResponse.fromJson(pointsResponseString)
        //Use the URL from the PointResponse to look up the forecast
        forecastResponseString <- client.expect[String](pointResponse.forecastUri).adaptError{ case t => WeatherError(coordinates,t)}
      } yield {
        Weather.fromJson(forecastResponseString)
      }

    case class PointResponse(forecastUri: Uri):
      def forecastRequest:Request[F] = GET(forecastUri)


    object PointResponse:

      def fromJson(jsonString: String): PointResponse =
        import io.circe.parser.parse
        import io.circe.Json
        import cats.syntax.either._

        val cursor = parse(jsonString).getOrElse(Json.Null).hcursor //todo decide about error handling
        val uriString:Decoder.Result[String] = cursor.downField("properties").downField("forecast").as[String]
        uriString.flatMap(Uri.fromString).map(PointResponse(_)).valueOr(t => throw t)
      //todo tidy up with a custom EntityDecoder

case class Weather(shortForecast:String,temperature:Int):
  def temperatureWord =
    ??? //convert the given temperature value into a terse string

object Weather:
  def fromJson(jsonString: String): Weather =
    import io.circe.parser.parse
    import io.circe.Json
    import cats.syntax.either._

    val cursor = parse(jsonString).getOrElse(Json.Null).hcursor //todo decide about error handling
    
    val firstPeriod = cursor.downField("properties").downField("periods").downArray
    val temperature: Int = firstPeriod.downField("temperature").as[Int].valueOr(t => throw t)
    val shortForecast: String = firstPeriod.downField("shortForecast").as[String].valueOr(t => throw t)
    Weather(shortForecast, temperature)
//todo tidy up with a custom EntityDecoder

case class Coordinates(lat:Double,lon:Double):
  def forUrl:String = s"$lat,$lon"

case class WeatherError(coordinates: Coordinates,e: Throwable) extends RuntimeException