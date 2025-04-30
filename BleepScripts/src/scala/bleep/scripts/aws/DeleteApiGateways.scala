package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.services.apigateway.model.DeleteRestApiRequest

import scala.jdk.CollectionConverters.ListHasAsScala

object DeleteApiGateways extends BleepScript("DeleteApiGateways") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val listResponse = CommonAws.apiGatewayClient.getRestApis()

    listResponse.items().asScala
      .filter{restApi => restApi.name() == "duck-update-api"}
      .sortBy(_.createdDate())
      .foreach{restApi =>
        println(s"${restApi.name()} ${restApi.id()}")
        val request = DeleteRestApiRequest.builder()
          .restApiId(restApi.id())
          .build()
        val response = CommonAws.apiGatewayClient.deleteRestApi(request)
        println(response)
      }


