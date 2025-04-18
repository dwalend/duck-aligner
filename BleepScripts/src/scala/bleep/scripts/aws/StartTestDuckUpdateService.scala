package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.services.apigateway.model.{ApiKeySourceType, ConnectionType, CreateDeploymentRequest, CreateResourceRequest, CreateRestApiRequest, EndpointConfiguration, EndpointType, GetResourcesRequest, GetStageRequest, IntegrationType, PutIntegrationRequest, PutIntegrationResponseRequest, PutMethodRequest, TestInvokeMethodRequest}
import software.amazon.awssdk.services.ec2.model.{LaunchTemplateSpecification, ResourceType, RunInstancesRequest, Tag, TagSpecification}

import scala.jdk.CollectionConverters.MapHasAsJava
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.annotation.tailrec

object StartTestDuckUpdateService extends BleepScript("StartTestDuckUpdateService") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =
    startEc2Machine()
    createApiGateway()

  private def startEc2Machine(): Unit =
    val launchTemplateSpecification = LaunchTemplateSpecification.builder()
      .launchTemplateName(CommonAws.launchTemplateName)
      .build()

    val startScript =
      s"""#!/bin/bash -x
         |sudo yum --assumeyes install java-21-amazon-corretto-headless
         |sudo yum --assumeyes upgrade
         |""".stripMargin

    val startBase64 = Base64.getEncoder.encodeToString(startScript.getBytes(StandardCharsets.UTF_8))

    val tagSpecification = TagSpecification.builder()
      .tags(CommonAws.tag)
      .resourceType(ResourceType.INSTANCE)
      .build()

    val runInstancesRequest = RunInstancesRequest.builder()
      .launchTemplate(launchTemplateSpecification)
      .minCount(1)
      .maxCount(1)
      .userData(startBase64)
      .tagSpecifications(tagSpecification)
      .build()
    val runInstanceResponse = CommonAws.ec2Client.runInstances(runInstancesRequest)
    println(runInstanceResponse)


  private def createApiGateway():Unit =
    val endpointConfiguration = EndpointConfiguration.builder()
      .types(EndpointType.REGIONAL)
      .build

    val createRestApiRequest = CreateRestApiRequest.builder()
      .name("duck-update-api")
      .apiKeySource(ApiKeySourceType.HEADER)
      .endpointConfiguration(endpointConfiguration)
      .build()

    val createRestApiResponse = CommonAws.apiGatewayClient.createRestApi(createRestApiRequest)

    println(createRestApiResponse)
    val restApiId = createRestApiResponse.id()

    val getResourcesRequest = GetResourcesRequest.builder()
      .restApiId(restApiId)
      .build()

    val getResourcesResponse = CommonAws.apiGatewayClient.getResources(getResourcesRequest)

    val createResourceRequest = CreateResourceRequest.builder()
      .restApiId(restApiId)
      .parentId(getResourcesResponse.items().get(0).id())
      .pathPart("{proxy+}")
      .build()

    val createResourceResponse = CommonAws.apiGatewayClient.createResource(createResourceRequest)
    println(createResourceResponse)

    val methodRequestParameters = Map("method.request.path.proxy" -> java.lang.Boolean.TRUE).asJava
    val putMethodRequest = PutMethodRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .httpMethod("ANY")
      .authorizationType("NONE")
      .requestParameters(methodRequestParameters)
      .build()
    val putMethodResponse = CommonAws.apiGatewayClient.putMethod(putMethodRequest)
    println(putMethodResponse)

    val requestParameters = Map("integration.request.path.proxy" -> "method.request.path.proxy").asJava

    //wait for an IP address
    @tailrec
    def pollForIp:String = {
      println("Polling for IP")
      val duckServerIp: String = CommonAws.describeTestInstances().map(_.publicIpAddress()).head
      if(duckServerIp != null) duckServerIp
      else
        Thread.sleep(5000)
        pollForIp
    }
    
    val duckServerIp: String = pollForIp
    println(s"duckServerIp $duckServerIp")

    val putIntegrationRequest = PutIntegrationRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .`type`(IntegrationType.HTTP_PROXY)
      .integrationHttpMethod("ANY")
      .httpMethod("ANY")
      .uri(s"http://$duckServerIp:8080/{proxy}")
      .connectionType(ConnectionType.INTERNET)
      .requestParameters(requestParameters)
      .passthroughBehavior("WHEN_NO_TEMPLATES")
      .build()

    val putIntegrationResponse = CommonAws.apiGatewayClient.putIntegration(putIntegrationRequest)
    println(putIntegrationResponse)

    val responseTemplates = Map("application/json" -> "null").asJava
    val putIntegrationResponseRequest = PutIntegrationResponseRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .httpMethod("ANY")
      .statusCode("200")
      .responseTemplates(responseTemplates)
      .build()
    val putIntegrationResponseResponse = CommonAws.apiGatewayClient.putIntegrationResponse(putIntegrationResponseRequest)
    println(putIntegrationResponseResponse)

    /*
    val testRequest = TestInvokeMethodRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .httpMethod("GET")
      .pathWithQueryString("bing")
      .build()
    val testResponse = CommonAws.apiGatewayClient.testInvokeMethod(testRequest)
    //todo something more interesting with the test response
    println(testResponse)
  */
    val stageName = "dev"

    val createDeploymentRequest = CreateDeploymentRequest.builder()
      .restApiId(restApiId)
      .stageName(stageName) //todo can I leave this out for now?
      .build()
    val createDeploymentResponse = CommonAws.apiGatewayClient.createDeployment(createDeploymentRequest)
    println(createDeploymentResponse)

    val getStageRequest = GetStageRequest.builder()
      .restApiId(restApiId)
      .stageName(stageName)
      .build()
    val getStageResponse = CommonAws.apiGatewayClient.getStage(getStageRequest)
    println(getStageResponse)

    val url = s"https://$restApiId.execute-api.${CommonAws.region.id()}.amazonaws.com/$stageName"
    println(url)
