package net.walend.duckaligner.duckupdateservice.awssdklocation

import cats.effect.{Async, IO}
import software.amazon.awssdk.services.geomaps.GeoMapsAsyncClient
import software.amazon.awssdk.services.geomaps.model.GetTileRequest

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object AwsLocationClient {
  
  val builder = GeoMapsAsyncClient.builder()

  val client: GeoMapsAsyncClient = builder
    .build()
  
  def getTile[F[_] <: Async[F]]() =
    
    val lon = 42.33588581370238
    val lat = -71.20792615771647

    val tileRequest: GetTileRequest = GetTileRequest.builder()
                                      .key(AwsSecrets.apiKey)
                                      .x("1") //0 to 3
                                      .y("1") //0 to 3
                                      .z("2")
                                      .tileset("vector.basemap")
                                      .build()
    
    IO.blocking(client.getTile(tileRequest).get())
}
