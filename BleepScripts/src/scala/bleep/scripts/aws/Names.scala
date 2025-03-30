package bleep.scripts.aws

import software.amazon.awssdk.services.ec2.model.Tag


/**
 *
 *
 * @author David Walend
 * @since v0.0.0
 */
object Names:
  val launchTemplateName = "duck-update-service-launch-template"

  val tagKey = "Name"
  val tagValue = "DuckTest"

  val tag = Tag.builder()
    .key(tagKey)
    .value(tagValue)
    .build()

