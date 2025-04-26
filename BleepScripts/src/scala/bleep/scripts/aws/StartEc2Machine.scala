package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.services.ec2.model.{LaunchTemplateSpecification, ResourceType, RunInstancesRequest, Tag, TagSpecification}

import java.util.Base64
import java.nio.charset.StandardCharsets

object StartEc2Machine extends BleepScript("CreateEc2LaunchTemplate") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

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
