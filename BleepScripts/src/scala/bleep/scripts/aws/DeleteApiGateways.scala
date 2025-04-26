package bleep.scripts.aws

import bleep.model.{CrossProjectName, ProjectName, ScriptName}
import bleep.packaging.dist
import bleep.scripts.fatjar.FatJar
import bleep.{BleepScript, Commands, PathOps, ProjectPaths, Started, model}
import bloop.config.Config
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model.{ApiKeySourceType, ConnectionType, CreateResourceRequest, CreateRestApiRequest, DeleteRestApiRequest, EndpointConfiguration, EndpointType, GetBasePathMappingRequest, GetDeploymentsRequest, GetIntegrationRequest, GetMethodRequest, GetResourcesRequest, GetRestApiRequest, GetRestApisRequest, IntegrationType, PutIntegrationRequest, PutIntegrationResponseRequest, PutMethodRequest}
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.{AuthorizeSecurityGroupIngressRequest, CreateImageRequest, CreateImageResponse, CreateLaunchTemplateRequest, CreateSecurityGroupRequest, LaunchTemplateBlockDeviceMappingRequest, LaunchTemplateEbsBlockDeviceRequest, LaunchTemplateSpecification, RequestLaunchTemplateData, RunInstancesRequest, StartInstancesRequest, VolumeType}
import software.amazon.awssdk.services.imagebuilder.model.{ComponentConfiguration, CreateComponentRequest, CreateImagePipelineRequest, CreateImageRecipeRequest, EbsInstanceBlockDeviceSpecification, EbsVolumeType, InstanceBlockDeviceMapping, ListImagesRequest, Platform, StartImagePipelineExecutionRequest}
import software.amazon.awssdk.services.imagebuilder.{ImagebuilderClient, ImagebuilderClientBuilder}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, PutObjectRequest, PutObjectResponse}

import java.nio.file.Path
import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala, MapHasAsJava}

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


