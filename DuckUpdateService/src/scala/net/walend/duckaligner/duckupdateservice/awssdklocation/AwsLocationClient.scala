package net.walend.duckaligner.duckupdateservice.awssdklocation

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

  val client: GeoMapsAsyncClient = builder.build()
  
  def getTile() =

    val tileRequest: GetTileRequest = GetTileRequest.builder()
                                      
                                      .build()
    
    client.getTile(tileRequest)

}
