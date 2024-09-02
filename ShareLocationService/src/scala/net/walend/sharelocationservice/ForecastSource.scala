package net.walend.sharelocationservice

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.implicits.uri
import org.http4s.{Request, Uri}

import scala.util.Try
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.GET

/**
 * Source of weather forecasts from The National Forecast Service API Web Service https://www.weather.gov/documentation/services-web-api
 *
 * To get from lat,lon to a short forecast is two steps. First touch a url like https://api.weather.gov/points/38.8894,-77.0352 to get the URL for the forecast - which will look like https://api.weather.gov/gridpoints/LWX/97,71/forecast . Touch that to find the "shortForecast" and "temperature"
 *
 */

trait ForecastSource[F[_]]:
  /**
   * @param coordinates lat and lon to use for the forecast
   * @return the weather forecast for those coordinates
   */
  def get(coordinates:Coordinates):F[Forecast]

object ForecastSource:
  def apply[F[_]](using ev:ForecastSource[F]): ForecastSource[F] = ev

  def forecastSource[F[_]: Concurrent](client: Client[F]):ForecastSource[F] = new ForecastSource[F]:
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    import dsl.*

    override def get(coordinates: Coordinates): F[Forecast] =
      //For the coordinates look up the right PointResponse
      val pointsRequest: Request[F] = GET(uri"https://api.weather.gov/points" / coordinates.forPointsUrl)
      println(pointsRequest)
      val forecastF: F[Forecast] = for
        pointsResponseString: String <- client.expect[String](pointsRequest)
        pointResponse:PointResponse = PointResponse.fromJson(pointsResponseString)
        //Use the URL from the PointResponse to look up the forecast
        forecastResponseString:String <- client.expect[String](pointResponse.forecastUri)
      yield Forecast.fromJson(forecastResponseString)
      forecastF.adaptError { case t => ForecastSourceError(coordinates, t) }

    case class PointResponse(forecastUri: Uri):
      def forecastRequest:Request[F] = GET(forecastUri)

    object PointResponse:

      def fromJson(jsonString: String): PointResponse =
        import io.circe.parser.parse
        import io.circe.Json
        import cats.syntax.either._

        val cursor = parse(jsonString).getOrElse(Json.Null).hcursor //todo decide about error handling
        val uriString:String = cursor.downField("properties").downField("forecast").as[String].valueOr(t => throw t)
        PointResponse(Uri.fromString(uriString).valueOr(t => throw t))
      //todo tidy up with a custom EntityDecoder

case class Forecast(shortForecast:String, temperature:Int):
  private def temperatureWord:String =
    val maxTempToWord:Seq[(Int,String)] = Seq (
      45 -> "Cold",
      80 -> "Moderate",
    )
    maxTempToWord.collectFirst{
      case tempToWord if temperature <= tempToWord._1 => tempToWord._2
    }.getOrElse("Hot")

  def toResponseString = s"$temperatureWord and $shortForecast"

object Forecast:
  def fromJson(jsonString: String): Forecast =
    import io.circe.parser.parse
    import io.circe.Json
    import cats.syntax.either._

    val cursor = parse(jsonString).getOrElse(Json.Null).hcursor //todo decide about error handling
    
    val firstPeriod = cursor.downField("properties").downField("periods").downArray
    val temperature: Int = firstPeriod.downField("temperature").as[Int].valueOr(t => throw t)
    val shortForecast: String = firstPeriod.downField("shortForecast").as[String].valueOr(t => throw t)
    Forecast(shortForecast, temperature)
//todo tidy up with a custom EntityDecoder

case class Coordinates(lat:Double,lon:Double):

  /**
   * Trims the lat and lon to match the national forecast service
   *
   "status": 301,
   "detail": "The precision of latitude/longitude points is limited to 4 decimal digits for efficiency. The location attribute contains your request mapped to the nearest supported point. If your client supports it, you will be redirected."
  ...
   "detail": "The coordinates cannot have trailing zeros in the decimal digit. The location attribute contains your request with the redundancy removed. If your client supports it, you will be redirected."
   */
  def forPointsUrl:String =
    //there's no way to strip trailing zeros with a scala f-string
    def formatForNWS(d:Double):String =
      val fourDecimals = f"$d%1.4f"
      fourDecimals.reverse.dropWhile(_ == '0').dropWhile(_ == '.').reverse

    s"${formatForNWS(lat)},${formatForNWS(lon)}"

object Coordinates:
  def unapply(string: String): Option[Coordinates] =
    val parts: Array[String] = string.split(',')
    parts match
      case Array(latString,lonString) => Try { Coordinates(latString.toDouble,lonString.toDouble) }.toOption
      case _ => None

case class ForecastSourceError(coordinates: Coordinates, e: Throwable) extends RuntimeException(s"with $coordinates",e)