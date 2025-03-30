package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.{LaunchTemplateSpecification, RunInstancesRequest, TerminateInstancesRequest}

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object TerminateEc2Machine extends BleepScript("CreateEc2Ami") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val ec2Client = Ec2Client.builder()
      .region(Region.US_EAST_1)
      .build()

    val describeInstancesResponse = ec2Client.describeInstances()
    println(describeInstancesResponse)

    //find the one with the right tag and terminate that one
    val instanceIds: Seq[String] = describeInstancesResponse.reservations().asScala.flatMap{ r =>
      r.instances().asScala.filter{i =>
        i.tags().contains(Names.tag)
      }
    }.map(_.instanceId()).toSeq

    println(instanceIds)

    val terminateInstancesRequest = TerminateInstancesRequest.builder()
      .instanceIds(instanceIds.asJava)
      .build()

    val terminateInstanceResponse = ec2Client.terminateInstances(terminateInstancesRequest)
    println(terminateInstanceResponse)
