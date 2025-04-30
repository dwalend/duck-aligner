package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model.{ApiKeySourceType, ConnectionType, CreateDeploymentRequest, CreateResourceRequest, CreateRestApiRequest, EndpointConfiguration, EndpointType, GetResourcesRequest, GetStageRequest, IntegrationType, PutIntegrationRequest, PutIntegrationResponseRequest, PutMethodRequest, TestInvokeMethodRequest}

import scala.jdk.CollectionConverters.MapHasAsJava

object CreateApiGateway extends BleepScript("CreateApiGateway") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val region = Region.US_EAST_1
    val stageName = "dev"
    val duckServiceIp = "54.83.71.47"

    lazy val apiGatewayClient = ApiGatewayClient.builder()
      .region(region)
      .build()

    val endpointConfiguration = EndpointConfiguration.builder()
      .types(EndpointType.REGIONAL)
      .build

    val createRestApiRequest = CreateRestApiRequest.builder()
      .name("duck-update-api")
      .apiKeySource(ApiKeySourceType.HEADER)
      .endpointConfiguration(endpointConfiguration)
      .build()

    val createRestApiResponse = apiGatewayClient.createRestApi(createRestApiRequest)

    println(createRestApiResponse)
    val restApiId = createRestApiResponse.id()

    val getResourcesRequest = GetResourcesRequest.builder()
      .restApiId(restApiId)
      .build()

    val getResourcesResponse = apiGatewayClient.getResources(getResourcesRequest)

    val createResourceRequest = CreateResourceRequest.builder()
      .restApiId(restApiId)
      .parentId(getResourcesResponse.items().get(0).id())
      .pathPart("{proxy+}")
      .build()

    val createResourceResponse = apiGatewayClient.createResource(createResourceRequest)
    println(createResourceResponse)

    val methodRequestParameters = Map("method.request.path.proxy" -> java.lang.Boolean.TRUE).asJava
    val putMethodRequest = PutMethodRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .httpMethod("ANY")
      .authorizationType("NONE")
      .requestParameters(methodRequestParameters)
      .build()
    val putMethodResponse = apiGatewayClient.putMethod(putMethodRequest)
    println(putMethodResponse)

    val requestParameters = Map("integration.request.path.proxy" -> "method.request.path.proxy").asJava
    val putIntegrationRequest = PutIntegrationRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .`type`(IntegrationType.HTTP_PROXY)
      .integrationHttpMethod("ANY")
      .httpMethod("ANY")
      .uri(s"http://$duckServiceIp:8080/{proxy}")
      .connectionType(ConnectionType.INTERNET)
      .requestParameters(requestParameters)
      .passthroughBehavior("WHEN_NO_TEMPLATES")
      .build()

    val putIntegrationResponse = apiGatewayClient.putIntegration(putIntegrationRequest)
    println(putIntegrationResponse)

    val responseTemplates = Map("application/json" -> "null").asJava
    val putIntegrationResponseRequest = PutIntegrationResponseRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .httpMethod("ANY")
      .statusCode("200")
      .responseTemplates(responseTemplates)
      .build()
    val putIntegrationResponseResponse = apiGatewayClient.putIntegrationResponse(putIntegrationResponseRequest)
    println(putIntegrationResponseResponse)

    val testRequest = TestInvokeMethodRequest.builder()
      .restApiId(restApiId)
      .resourceId(createResourceResponse.id())
      .httpMethod("GET")
      .pathWithQueryString("bing")
      .build()
    val testResponse = apiGatewayClient.testInvokeMethod(testRequest)
    //todo something more interesting with the test response
    println(testResponse)

    val createDeploymentRequest = CreateDeploymentRequest.builder()
      .restApiId(restApiId)
      .stageName(stageName)
      .build()
    val createDeploymentResponse = apiGatewayClient.createDeployment(createDeploymentRequest)
    println(createDeploymentResponse)

    val getStageRequest = GetStageRequest.builder()
      .restApiId(restApiId)
      .stageName(stageName)
      .build()
    val getStageResponse = apiGatewayClient.getStage(getStageRequest)
    println(getStageResponse)

    val url = s"https://$restApiId.execute-api.${region.id()}.amazonaws.com/$stageName"
    println(url)