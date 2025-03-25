package bleep.scripts.ami

import bleep.model.{CrossProjectName, ProjectName, ScriptName}
import bleep.packaging.dist
import bleep.scripts.fatjar.FatJar
import bleep.{BleepScript, Commands, PathOps, ProjectPaths, Started, model}
import bloop.config.Config
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.{AuthorizeSecurityGroupIngressRequest, CreateImageRequest, CreateImageResponse, CreateLaunchTemplateRequest, CreateSecurityGroupRequest, LaunchTemplateBlockDeviceMappingRequest, LaunchTemplateEbsBlockDeviceRequest, LaunchTemplateSpecification, RequestLaunchTemplateData, RunInstancesRequest, StartInstancesRequest, VolumeType}
import software.amazon.awssdk.services.imagebuilder.{ImagebuilderClient, ImagebuilderClientBuilder}
import software.amazon.awssdk.services.imagebuilder.model.{ComponentConfiguration, CreateComponentRequest, CreateImagePipelineRequest, CreateImageRecipeRequest, EbsInstanceBlockDeviceSpecification, EbsVolumeType, InstanceBlockDeviceMapping, ListImagesRequest, Platform, StartImagePipelineExecutionRequest}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, PutObjectRequest, PutObjectResponse}

import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala

object CreateEc2Ami extends BleepScript("CreateEc2Ami") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val projectName = ProjectName(args.head)
    val crossProjectName: CrossProjectName = model.CrossProjectName(projectName,None)

    val bloopProject: Config.Project = started.bloopFiles(crossProjectName).forceGet.project
    val mainClassName: Option[String] = bloopProject.platform.flatMap(_.mainClass)
    val program = dist.Program(crossProjectName.value, mainClassName.get)

    commands.script(ScriptName("fat-jar"),args)

    val ec2Client = Ec2Client.builder()
      .region(Region.US_EAST_1)
      .build()

    val securityGroupName = "Duck Update Server Security"

    /*
    val createSecurityGroupRequest = CreateSecurityGroupRequest.builder()
      .groupName(securityGroupName)
      .description("ssh and port 8080 for the Duck Update service")
      .build()

    val createSecurityGroupResponse = ec2Client.createSecurityGroup(createSecurityGroupRequest)
    println(createSecurityGroupResponse)

    val authorizeIngressRequest = AuthorizeSecurityGroupIngressRequest.builder()
      .groupName(securityGroupName)
      .ipProtocol("tcp")
      .toPort(22)
      .fromPort(22)
      .cidrIp("74.104.110.185/32")
      .build
    val authorizeIngressResponse = ec2Client.authorizeSecurityGroupIngress(authorizeIngressRequest)
    println(authorizeIngressResponse)
*/
    /* All this to make the launch template
    val ebsInstanceBlockDeviceSpecification = LaunchTemplateEbsBlockDeviceRequest.builder()
      .deleteOnTermination(true)
      .volumeSize(8)
      .volumeType(VolumeType.GP3)
      //      .snapshotId("snap-041f7252be0b80435") //pulled this out of the UI. No idea where else to find it
      .build()

    val blockDeviceMapping = LaunchTemplateBlockDeviceMappingRequest.builder()
      .ebs(ebsInstanceBlockDeviceSpecification)
      .deviceName("/dev/xvda")
      .build()

    val requestLaunchTemplateData = RequestLaunchTemplateData.builder()
      .keyName("davidAtWalendDotNet")
      .imageId("ami-0eae2a0fc13b15fce")
      .instanceType("t4g.nano")
      .securityGroups(securityGroupName)
      .ebsOptimized(true)
      .blockDeviceMappings(blockDeviceMapping)
      .build()

    val launchTemplateName = "duck-update-service-launch-template"
    val createLaunchTemplateRequest = CreateLaunchTemplateRequest.builder()
      .launchTemplateName(launchTemplateName)
      .launchTemplateData(requestLaunchTemplateData)
      .build()

    val createLaunchTemplateResponse = ec2Client.createLaunchTemplate(createLaunchTemplateRequest)
    println(createLaunchTemplateResponse)
*/
    val launchTemplateName = "duck-update-service-launch-template"
    val launchTemplateSpecification = LaunchTemplateSpecification.builder()
      .launchTemplateName(launchTemplateName)
      .build()

    val runInstancesRequest = RunInstancesRequest.builder()
      .launchTemplate(launchTemplateSpecification)
      .minCount(1)
      .maxCount(1)
      .build()
    val runInstanceResponse = ec2Client.runInstances(runInstancesRequest)
    println(runInstanceResponse)

    //todo next look up the public IP
    //todo how to execute script via ssh?
    //todo sudo yum install java-21-amazon-corretto-headless
    //todo yum update/upgrade
    //todo split what happens next?
    //todo scp the fat jar
    //todo start the service
    //todo or
    //todo rebake the image / make a new launch template?
    //todo less memory?

    /*
    //upload the fat jar to s3
    val s3Client = S3Client.builder()
      .region(Region.US_EAST_1)
      .build()

    //create the bucket
    //copy the fat jar into the bucket
    val bucketName = "duck-aligner-service-jar-bucket"

    val createBucketRequest = CreateBucketRequest.builder()
      .bucket(bucketName)
      .build()
    val createBucketResponse = s3Client.createBucket(createBucketRequest)
    println(createBucketResponse)

    val objectKey = projectName.value
    val putObjectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(objectKey)
      .build()
    val putObjectResponse: PutObjectResponse = s3Client.putObject(putObjectRequest,FatJar.jarPath(started, projectName))
    println(putObjectResponse)

    val s3ObjectArn = s"arn:aws:s3:::$bucketName/$objectKey"

    val version = "0.0.17"

    */

    //todo delete the bucket at the end


    //todo create an ec2 ami with AWS Linux, Latest LTS Corretto JDK (21), the fat jar, and a command line to start it on start-up ... maybe to use an argument from somewhere for who to let in
/*
    val imageBuilderClient = ImagebuilderClient.builder()
      .region(Region.US_EAST_1)
      .build()

    val corretto21Configuration = ComponentConfiguration.builder()
      .componentArn("arn:aws:imagebuilder:us-east-1:aws:component/amazon-corretto-21-headless/1.0.0/1")
      .build()
    */
/*
    val fatJarS3Yaml =
      s"""name: InstallDuckUpdateServiceFatJar
        |description: Download the DuckUpdateService Fat Jar.
        |schemaVersion: 1.0
        |phases:
        |  - name: build
        |    steps:
        |      - name: Download
        |        action: S3Download
        |        inputs:
        |          - source: s3://$bucketName/$objectKey
        |            destination: /tmp/$objectKey
        |      - name: Install
        |        action: ExecuteBinary
        |        onFailure: Continue
        |        inputs:
        |          path: java
        |          arguments:
        |            - '-jar {{ build.Download.inputs[0].destination }} &'
        |""".stripMargin

    println(fatJarS3Yaml)

    val createComponentRequest = CreateComponentRequest.builder()
      .name("InstallDuckUpdateServiceFatJar")
      .semanticVersion(version)
      .platform(Platform.LINUX)
      .data(fatJarS3Yaml)
      .build()

    //
    val createFatJarComponentResponse = imageBuilderClient.createComponent(createComponentRequest)
    println(createFatJarComponentResponse)

//    val createFatJarComponentArn = "arn:aws:imagebuilder:us-east-1:634303334930:component/installduckupdateservicefatjar/0.0.0/1"
    val fatJarConfiguration = ComponentConfiguration.builder()
      .componentArn(createFatJarComponentResponse.componentBuildVersionArn())
//      .componentArn(createFatJarComponentArn)
      .build()
*/
    /*
    val ebsInstanceBlockDeviceSpecification = EbsInstanceBlockDeviceSpecification.builder()
      .deleteOnTermination(false)
      .volumeSize(8)
//      .snapshotId("snap-041f7252be0b80435") //pulled this out of the UI. No idea where else to find it
      .build()

    val blockDeviceMapping = InstanceBlockDeviceMapping.builder()
      .ebs(ebsInstanceBlockDeviceSpecification)
      .deviceName("/dev/xvda")
      .build()

    val createImageRecipeRequest = CreateImageRecipeRequest.builder()
      .name("DuckUpdateImageRecipe")
      .semanticVersion(version)
      .parentImage("arn:aws:imagebuilder:us-east-1:aws:image/amazon-linux-2023-ecs-optimized-arm64/x.x.x") //latest amazon linux 2023
      .components(corretto21Configuration)
//todo maybe      .components(corretto21Configuration,fatJarConfiguration)
      .blockDeviceMappings(blockDeviceMapping)
      .build()

    val createImageRecipeResponse = imageBuilderClient.createImageRecipe(createImageRecipeRequest)
    println(createImageRecipeResponse)

//    val imageRecipeArn = "arn:aws:imagebuilder:us-east-1:634303334930:image-recipe/duckupdateimagerecipe/0.0.1"

    val createImagePipelineRequest = CreateImagePipelineRequest.builder()
      .name(s"DuckUpdateServerPipeline")
      .description("Duck Update Server Pipeline")
      .enhancedImageMetadataEnabled(true)
      .infrastructureConfigurationArn("arn:aws:imagebuilder:us-east-1:634303334930:infrastructure-configuration/test-pipeline-73b828b9-59be-424f-b705-7c4a2ee712e9") //this monstrosity is from the AWS console
      .imageRecipeArn(createImageRecipeResponse.imageRecipeArn())
//      .imageRecipeArn(imageRecipeArn)
      .build()

    val createImagePipelineResponse = imageBuilderClient.createImagePipeline(createImagePipelineRequest)
    
//    val imagePipelineArn = "arn:aws:imagebuilder:us-east-1:634303334930:image-pipeline/duckupdateserverpipeline"

    val startImagePipelineExecutionRequest = StartImagePipelineExecutionRequest.builder()
//      .imagePipelineArn(imagePipelineArn)
      .imagePipelineArn(createImagePipelineResponse.imagePipelineArn())
      .build()

    val startImagePipelineExecutionResponse = imageBuilderClient.startImagePipelineExecution(startImagePipelineExecutionRequest)
    println(startImagePipelineExecutionResponse)
    */
    /*
    val listImageRequest = ListImagesRequest.builder()
      .maxResults(10)
      .build()

    val listImageResponse = imageBuilderClient.listImages(listImageRequest)

    println(listImageResponse)

    val ec2Client = Ec2Client.builder()
      .region(Region.US_EAST_1)
      .build()

    val createImageRequest: CreateImageRequest = CreateImageRequest.builder()
      .instanceId("DuckUpdateService")
      .description("AMI for the DuckUpdateService")
      .name("DuckUpdateService")
      .noReboot(false)
      .dryRun(true)
      .build()

    val createImageResponse: CreateImageResponse = ec2Client.createImage(createImageRequest)

    println(createImageResponse)
    */


    

    