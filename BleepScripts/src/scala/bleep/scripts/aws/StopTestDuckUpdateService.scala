package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.services.apigateway.model.DeleteRestApiRequest
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object StopTestDuckUpdateService extends BleepScript("StopTestDuckUpdateService") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =
    deleteApiGateways()
    terminateEc2Machines()

  private def terminateEc2Machines(): Unit =
    val instanceIds = CommonAws.describeTestInstances()
      .filter(_.tags().asScala.contains(CommonAws.tag))
      .map(_.instanceId())
    println(instanceIds)

    val terminateInstancesRequest = TerminateInstancesRequest.builder()
      .instanceIds(instanceIds.asJava)
      .build()

    val terminateInstanceResponse = CommonAws.ec2Client.terminateInstances(terminateInstancesRequest)
    println(terminateInstanceResponse)

  private def deleteApiGateways(): Unit =
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