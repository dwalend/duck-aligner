package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.services.ec2.model.{AuthorizeSecurityGroupIngressRequest, CreateLaunchTemplateRequest, CreateSecurityGroupRequest, LaunchTemplateBlockDeviceMappingRequest, LaunchTemplateEbsBlockDeviceRequest, RequestLaunchTemplateData, VolumeType}

object CreateEc2LaunchTemplate extends BleepScript("CreateEc2LaunchTemplate") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val ec2Client = CommonAws.ec2Client

    val securityGroupName = "Duck Update Server Security"
    
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

    /// All this to make the launch template
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
      .keyName("davidAtWalendDotNet") //ssh key name
      .imageId("resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64") //from https://docs.aws.amazon.com/corretto/latest/corretto-25-ug/downloads-list.html
      .instanceType("t4g.nano")
      .securityGroups(securityGroupName)
      .ebsOptimized(true)
      .blockDeviceMappings(blockDeviceMapping)
      .build()

    val createLaunchTemplateRequest = CreateLaunchTemplateRequest.builder()
      .launchTemplateName(CommonAws.launchTemplateName)
      .launchTemplateData(requestLaunchTemplateData)
      .build()

    val createLaunchTemplateResponse = ec2Client.createLaunchTemplate(createLaunchTemplateRequest)
    println(createLaunchTemplateResponse)