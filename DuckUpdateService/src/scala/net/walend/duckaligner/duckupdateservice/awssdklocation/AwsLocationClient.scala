package net.walend.duckaligner.duckupdateservice.awssdklocation

import cats.effect.{Async, IO}
import software.amazon.awssdk.services.geomaps.GeoMapsAsyncClient
import software.amazon.awssdk.services.geomaps.model.{GetStaticMapRequest, GetStaticMapResponse, GetTileRequest, ScaleBarUnit, StaticMapStyle}

import java.util.concurrent.CompletableFuture

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object AwsLocationClient:

  val builder = GeoMapsAsyncClient.builder()

  val client: GeoMapsAsyncClient = builder
    .build()
  
  def getTile[F[_] <: Async[F]]() =


    val tileRequest: GetTileRequest = GetTileRequest.builder()
                                      .key(AwsSecrets.apiKey)
                                      .x("1") //0 to 3
                                      .y("1") //0 to 3
                                      .z("2")
                                      .tileset("vector.basemap")
                                      .build()
    //todo Use tagless final F[_]
    IO.blocking(client.getTile(tileRequest).get())

  val lat1 = 42.33588581370238
  val lon1 = -71.20792615771647

  val lat2 = 42.32588581370238
  val lon2 = -71.18792615771647

  val newton = Seq(lon1,lat1,lon2,lat2)

  val libraryGeoJson =
    s"""{
       |  "type": "FeatureCollection",
       |  "features": [
       |    {
       |      "type": "Feature",
       |      "geometry": {
       |        "type": "Point",
       |        "coordinates": [
       |          $lon1,
       |          $lat1
       |        ]
       |        },
       |      "properties": {
       |          "color":"#0000FF",
       |          "icon": "bubble",
       |          "size":"medium",
       |          "label":"Free Library",
       |          "text-color":"#FFFFFF"
       |       }
       |    },
       |    {
       |      "type": "Feature",
       |      "geometry": {
       |        "type": "Point",
       |        "coordinates": [$lon2, $lat2]
       |      },
       |      "properties": {
       |        "label": "Duck",
       |        "icon": "bubble",
       |        "color": "#FF9800",
       |        "outline-color": "#D76B0B",
       |        "text-color": "#FFFFFF"
       |      }
       |    }
       |  ]
       |}""".stripMargin

  def requestStaticMap: IO[GetStaticMapResponse] =
    val mapRequest = GetStaticMapRequest.builder()
      .key(AwsSecrets.apiKey)
      .boundedPositions(newton.mkString(","))
      .padding(50)
      .style(StaticMapStyle.SATELLITE)
      .height(1000)
      .width(1000)
      .scaleBarUnit(ScaleBarUnit.KILOMETERS_MILES)
      .geoJsonOverlay(libraryGeoJson)
      .fileName("map")
      .build()

    IO.blocking(client.getStaticMap(mapRequest).get)

