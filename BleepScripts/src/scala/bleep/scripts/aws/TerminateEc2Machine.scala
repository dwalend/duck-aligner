package bleep.scripts.aws

import bleep.{BleepScript, Commands, Started}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object TerminateEc2Machine extends BleepScript("CreateEc2LaunchTemplate") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val instanceIds = CommonAws.describeTestInstances().map(_.instanceId())
    println(instanceIds)

    val terminateInstancesRequest = TerminateInstancesRequest.builder()
      .instanceIds(instanceIds.asJava)
      .build()

    val terminateInstanceResponse = CommonAws.ec2Client.terminateInstances(terminateInstancesRequest)
    println(terminateInstanceResponse)
