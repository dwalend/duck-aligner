package net.walend.duckaligner.duckupdateservice.awssdklocation

import software.amazon.awssdk.services.geomaps.GeoMapsAsyncClient

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object AwsLocationClient {

  val builder = GeoMapsAsyncClient.builder()

  val client = builder.build()
  
  client.getTile()

}
