package net.walend.sharelocationservice

import cats.effect.{Async, Sync}
import cats.syntax.all.*
import io.circe.Decoder.Result
import io.circe.DecodingFailure.Reason.CustomReason
import io.circe.{Decoder, DecodingFailure, HCursor}
import org.http4s.implicits.uri
import org.http4s.{EntityDecoder, ParseFailure, Request, Response, Uri}

import scala.util.Try
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.GET
import org.http4s.circe.jsonOf

/**
 * Source of weather forecasts from The National Forecast Service API Web Service https://www.weather.gov/documentation/services-web-api
 *
 * Gets from lat,lon to a short forecast in two steps. First uses some url like https://api.weather.gov/points/38.8894,-77.0352 to get the URL for the forecast - which will look like https://api.weather.gov/gridpoints/LWX/97,71/forecast then uses that url to find the "shortForecast" and "temperature"
 */

trait ForecastSource[F[_]]:
  /**
   * @param coordinates lat and lon to use for the forecast
   * @return the weather forecast for those coordinates
   */
  def get(coordinates:Coordinates):F[Forecast]

object ForecastSource:
  def apply[F[_]](using ev:ForecastSource[F]): ForecastSource[F] = ev

  def forecastSource[F[_]: Async](client: Client[F]):ForecastSource[F] = new ForecastSource[F]:
    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    import dsl.*

    override def get(coordinates: Coordinates): F[Forecast] =
      val forecastF: F[Forecast] = for
        pointResponse: ForecastSelector <- getPointsResponse(coordinates)
        forecast:Forecast <- getForecast(pointResponse)
      yield forecast

      forecastF.adaptError { case t =>
        t.fillInStackTrace()
        val fse = ForecastSourceError(coordinates, t)
        fse.fillInStackTrace()
        fse
      }

    /**
     * @return the right ForecastSelector for the coordinates
     */
    private def getPointsResponse(coordinates: Coordinates): F[ForecastSelector] =
      import ForecastSelector.nwsDecoder
      given pointResponseDecoder: EntityDecoder[F, ForecastSelector] = jsonOf[F, ForecastSelector]
      val pointsRequest: Request[F] = GET(uri"https://api.weather.gov/points" / coordinates.forPointsUrl)

      client.expectOr[ForecastSelector](pointsRequest)(ForecastSourceResponseError.fromResponse(_))

    /**
     * @return the Forecast for the pointResponse
     */
    private def getForecast(pointResponse:ForecastSelector): F[Forecast] =
      import Forecast.nwsDecoder
      given forecastDecoder: EntityDecoder[F, Forecast] = jsonOf[F, Forecast]

      client.expectOr[Forecast](pointResponse.forecastUri)(ForecastSourceResponseError.fromResponse(_))


    case class ForecastSelector(forecastUri: Uri):
      def forecastRequest:Request[F] = GET(forecastUri)

    object ForecastSelector:
      implicit val nwsDecoder:Decoder[ForecastSelector] = new Decoder[ForecastSelector]{

        //noinspection ConvertExpressionToSAM
        val uriDecoder:Decoder[Uri] = new Decoder[Uri]{
          override def apply(c: HCursor): Result[Uri] =
            Uri.fromString(c.value.asString.get).leftMap { (parseFailure:ParseFailure) =>
              DecodingFailure(CustomReason(parseFailure.message),c)
            }
        }

        override def apply(c: HCursor): Result[ForecastSelector] =
          c.downField("properties").get[Uri]("forecast")(uriDecoder).map(ForecastSelector.apply)
      }

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

  //noinspection ConvertExpressionToSAM
  implicit val nwsDecoder: Decoder[Forecast] = new Decoder[Forecast] {
    override def apply(c: HCursor): Result[Forecast] =
      val firstPeriod = c.downField("properties").downField("periods").downArray

      for
        temperature <- firstPeriod.get[Int]("temperature")
        shortForecast <- firstPeriod.get[String]("shortForecast")
      yield Forecast(shortForecast, temperature)
    }

case class Coordinates(lat:Double,lon:Double):

  /**
   * Trims the lat and lon to match the national forecast service's requirements, from these redirect messages:
   *
   "status": 301,
   "detail": "The precision of latitude/longitude points is limited to 4 decimal digits for efficiency. The location attribute contains your request mapped to the nearest supported point. If your client supports it, you will be redirected."
  ...
   "status": 301,
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

case class ForecastSourceError(coordinates: Coordinates, x: Throwable) extends RuntimeException(s"with $coordinates",x)

case class ForecastResponseNotJsonError(notJson: String) extends RuntimeException(s"Circe could not parse $notJson")

//noinspection ScalaWeakerAccess
case class ForecastSourceResponseError(responseAsString: String) extends RuntimeException(s"NWS responded $responseAsString")

object ForecastSourceResponseError:
  def fromResponse[F[_]: Sync](response:Response[F]): F[Throwable] =
    val stringF: F[String] = response.body.through(fs2.text.utf8.decode).compile.string
    stringF.map{ body =>
      ForecastSourceResponseError(s"${response.status} $body")
    }
