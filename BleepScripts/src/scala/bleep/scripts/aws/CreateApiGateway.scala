package bleep.scripts.aws

import bleep.model.{CrossProjectName, ProjectName, ScriptName}
import bleep.packaging.dist
import bleep.scripts.fatjar.FatJar
import bleep.{BleepScript, Commands, PathOps, ProjectPaths, Started, model}
import bloop.config.Config
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model.{ApiKeySourceType, ConnectionType, CreateDeploymentRequest, CreateResourceRequest, CreateRestApiRequest, CreateStageRequest, EndpointConfiguration, EndpointType, GetBasePathMappingRequest, GetDeploymentsRequest, GetIntegrationRequest, GetMethodRequest, GetResourcesRequest, GetRestApiRequest, GetRestApisRequest, GetStageRequest, IntegrationType, PutIntegrationRequest, PutIntegrationResponseRequest, PutMethodRequest, TestInvokeMethodRequest}
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.{AuthorizeSecurityGroupIngressRequest, CreateImageRequest, CreateImageResponse, CreateLaunchTemplateRequest, CreateSecurityGroupRequest, LaunchTemplateBlockDeviceMappingRequest, LaunchTemplateEbsBlockDeviceRequest, LaunchTemplateSpecification, RequestLaunchTemplateData, RunInstancesRequest, StartInstancesRequest, VolumeType}
import software.amazon.awssdk.services.imagebuilder.model.{ComponentConfiguration, CreateComponentRequest, CreateImagePipelineRequest, CreateImageRecipeRequest, EbsInstanceBlockDeviceSpecification, EbsVolumeType, InstanceBlockDeviceMapping, ListImagesRequest, Platform, StartImagePipelineExecutionRequest}
import software.amazon.awssdk.services.imagebuilder.{ImagebuilderClient, ImagebuilderClientBuilder}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, PutObjectRequest, PutObjectResponse}

import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.jdk.CollectionConverters.MapHasAsJava

object CreateApiGateway extends BleepScript("CreateApiGateway") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    lazy val apiGatewayClient = ApiGatewayClient.builder()
      .region(Region.US_EAST_1)
      .build()

    //GetRestApisResponse(Items=[RestApi(Id=ku1gmkmsf7, Name=duck-test-5, Description=Try a Rest API, CreatedDate=2025-04-12T20:18:58Z, ApiKeySource=HEADER, EndpointConfiguration=EndpointConfiguration(Types=[REGIONAL], IpAddressType=ipv4), DisableExecuteApiEndpoint=false, RootResourceId=g48cvgxk0e)])
/**/
//    val tags = Map("Name" -> "DuckUpdateServiceApiGateway").asJava
    val endpointConfiguration = EndpointConfiguration.builder()
      .types(EndpointType.REGIONAL)
      .build

    val createRestApiRequest = CreateRestApiRequest.builder()
      .name("duck-update-api")
//      .tags(tags)
      .apiKeySource(ApiKeySourceType.HEADER)
      .endpointConfiguration(endpointConfiguration)
      .build()

    val createRestApiResponse = apiGatewayClient.createRestApi(createRestApiRequest)

    println(createRestApiResponse)

    val getResourcesRequest = GetResourcesRequest.builder()
      .restApiId(createRestApiResponse.id())
      .build()

    val getResourcesResponse = apiGatewayClient.getResources(getResourcesRequest)

//    GetResourcesResponse(Items=[Resource(Id=g48cvgxk0e, Path=/), Resource(Id=lio9qn, ParentId=g48cvgxk0e, PathPart={proxy+}, Path=/{proxy+}, ResourceMethods={ANY=Method()})])

    //todo figure out ResourceMethods={ANY=Method()}
    val createResourceRequest = CreateResourceRequest.builder()
      .restApiId(createRestApiResponse.id())
      .parentId(getResourcesResponse.items().get(0).id())
      .pathPart("{proxy+}")
      .build()

    val createResourceResponse = apiGatewayClient.createResource(createResourceRequest)
    println(createResourceResponse)
//GetMethodResponse(HttpMethod=ANY, AuthorizationType=NONE, ApiKeyRequired=false, RequestParameters={method.request.path.proxy=true}, MethodIntegration=Integration(Type=HTTP_PROXY, HttpMethod=ANY, Uri=http://54.83.71.47:8080/{proxy}, ConnectionType=INTERNET, RequestParameters={integration.request.path.proxy=method.request.path.proxy}, PassthroughBehavior=WHEN_NO_TEMPLATES, TimeoutInMillis=29000, CacheNamespace=lio9qn, CacheKeyParameters=[], IntegrationResponses={200=IntegrationResponse(StatusCode=200, ResponseTemplates={application/json=null})}))

    val methodRequestParameters = Map("method.request.path.proxy" -> java.lang.Boolean.TRUE).asJava
    val putMethodRequest = PutMethodRequest.builder()
      .restApiId(createRestApiResponse.id())
      .resourceId(createResourceResponse.id())
      .httpMethod("ANY")
      .authorizationType("NONE")
      .requestParameters(methodRequestParameters)
      .build()
    val putMethodResponse = apiGatewayClient.putMethod(putMethodRequest)
    println(putMethodResponse)

    //todo start here - I think I have to make something else first

    //GetIntegrationResponse(Type=HTTP_PROXY, HttpMethod=ANY, Uri=http://54.83.71.47:8080/{proxy}, ConnectionType=INTERNET, RequestParameters={integration.request.path.proxy=method.request.path.proxy}, PassthroughBehavior=WHEN_NO_TEMPLATES, TimeoutInMillis=29000, CacheNamespace=lio9qn, CacheKeyParameters=[], IntegrationResponses={200=IntegrationResponse(StatusCode=200, ResponseTemplates={application/json=null})})
//    val requestParameters = Map("integration.request.path.proxy" -> "method.request.path.proxy").asJava
    val requestParameters = Map("integration.request.path.proxy" -> "method.request.path.proxy").asJava
    val putIntegrationRequest = PutIntegrationRequest.builder()
      .restApiId(createRestApiResponse.id())
      .resourceId(createResourceResponse.id())
      .`type`(IntegrationType.HTTP_PROXY)
      .integrationHttpMethod("ANY")
      .httpMethod("ANY")
      .uri("http://54.83.71.47:8080/{proxy}")
      .connectionType(ConnectionType.INTERNET)
      .requestParameters(requestParameters)
      .passthroughBehavior("WHEN_NO_TEMPLATES")
      .build()

    val putIntegrationResponse = apiGatewayClient.putIntegration(putIntegrationRequest)
    println(putIntegrationResponse)

    val responseTemplates = Map("application/json" -> "null").asJava
    val putIntegrationResponseRequest = PutIntegrationResponseRequest.builder()
      .restApiId(createRestApiResponse.id())
      .resourceId(createResourceResponse.id())
      .httpMethod("ANY")
      .statusCode("200")
      .responseTemplates(responseTemplates)
      .build()
    val putIntegrationResponseResponse = apiGatewayClient.putIntegrationResponse(putIntegrationResponseRequest)
    println(putIntegrationResponseResponse)

    val testRequest = TestInvokeMethodRequest.builder()
      .restApiId(createRestApiResponse.id())
      .resourceId(createResourceResponse.id())
      .httpMethod("GET")
      .pathWithQueryString("bing")
      .build()
    val testResponse = apiGatewayClient.testInvokeMethod(testRequest)
    println(testResponse)

    val createDeploymentRequest = CreateDeploymentRequest.builder()
      .restApiId(createRestApiResponse.id())
      .stageName("dev")
      .build()
    val createDeploymentResponse = apiGatewayClient.createDeployment(createDeploymentRequest)
    println(createDeploymentResponse)

    val getStageRequest = GetStageRequest.builder()
      .restApiId(createRestApiResponse.id())
      .stageName("dev")
      .build()
    val getStageResponse = apiGatewayClient.getStage(getStageRequest)
    println(getStageResponse)

    val url = s"https://{{restApiId}}.execute-api.{{region}}.amazonaws.com/{{stageName}}"
    println(url)

    /* todo maybe don't need this
    val createStageRequest = CreateStageRequest.builder()
      .restApiId(createRestApiResponse.id())
      .stageName("dev")
      .build()
    val createStageResponse = apiGatewayClient.createStage(createStageRequest)
    println(createStageResponse)
    */
/**/

    def describeApi(apiId:String,resourceId:String) =
      println()
      println(s"Parts for $apiId $resourceId")

      val req0 = GetRestApiRequest.builder()
        .restApiId(apiId)
        .build()
      val resp0 = apiGatewayClient.getRestApi(req0)
      println(resp0)
      describeResource(resourceId)

      def describeResource(resourceId:String) =
        println()
        val req1 = GetResourcesRequest.builder()
          .restApiId(apiId)
          .build()
        val resp1 = apiGatewayClient.getResources(req1)
        println(resp1)

        //todo check top-level resource

        val req2 = GetIntegrationRequest.builder()
          .restApiId(apiId)
          .resourceId(resourceId)
          .httpMethod("ANY")
          .build()
        val resp2 = apiGatewayClient.getIntegration(req2)
        println(resp2)

        val req3 = GetMethodRequest.builder()
          .restApiId(apiId)
          .resourceId(resourceId)
          .httpMethod("ANY")
          .build()
        val resp3 = apiGatewayClient.getMethod(req3)
        println(resp3)

        println()
    /*
    val workingApiId = "ku1gmkmsf7"
    val workingResource = "lio9qn"
    describeApi(workingApiId,workingResource)

    describeApi(createRestApiResponse.id(), createResourceResponse.id())
    */