package bleep.scripts.aws

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.{Instance, Tag}

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object CommonEc2:
  lazy val ec2Client = Ec2Client.builder()
    .region(Region.US_EAST_1)
    .build()

  val launchTemplateName = "duck-update-service-launch-template"

  val tagKey = "Name"
  val tagValue = "DuckTest"

  val tag = Tag.builder()
    .key(tagKey)
    .value(tagValue)
    .build()

  def describeTestInstances():Seq[Instance] =
    val describeInstancesResponse = ec2Client.describeInstances()
//    println(describeInstancesResponse)

    //find the one with the right tag and terminate that one
    describeInstancesResponse.reservations().asScala.flatMap { r =>
      r.instances().asScala.filter { i =>
        i.tags().contains(CommonEc2.tag)
      }
    }.toSeq

