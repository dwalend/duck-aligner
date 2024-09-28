package net.walend.sharelocationservice

import cats.effect.{Async, Sync}
import cats.syntax.all.*
import io.circe.Decoder
import org.http4s.implicits.uri
import org.http4s.{Request, Response, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method.GET
import smithy4s.aws.{AwsEnvironment, AwsRegion}


/**
 * Source of geo tiles
 *
 * Gets a map tile that holds all of a collection of lat,lon
 */



trait GeoTileSource[F[_]]:

  def get(coordinates:Seq[Coordinates]):F[Tile]

object GeoTileSource:
  def apply[F[_]](using ev:GeoTileSource[F]): GeoTileSource[F] = ev

  def geoTileSource[F[_]: Async](client: Client[F]):GeoTileSource[F] = new GeoTileSource[F]:
    AwsEnvironment.default(client,AwsRegion.US_EAST_1).use { env: AwsEnvironment[F] =>
      ???
      
    }

    val dsl: Http4sClientDsl[F] = new Http4sClientDsl[F]{}
    import dsl.*

    override def get(coordinates:Seq[Coordinates]): F[Tile] =
      ???

    case class PointResponse(forecastUri: Uri):
      def forecastRequest:Request[F] = GET(forecastUri)

    object PointResponse:

      def fromJson(jsonString: String): PointResponse =
        import io.circe.parser.parse
        import cats.syntax.either._

        val cursor = parse(jsonString).getOrElse(throw ForecastResponseNotJsonError(jsonString)).hcursor
        val uriString:String = cursor.downField("properties").downField("forecast").as[String].valueOr(t => throw t)
        PointResponse(Uri.fromString(uriString).valueOr(t => throw t))

case class Tile(coordinates: Coordinates)

