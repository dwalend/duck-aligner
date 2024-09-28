import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::0.18.24`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._, publish._
object Smithy4sAwsClient extends ScalaModule with Smithy4sModule with PublishModule {
  import smithy4s.codegen.AwsSpecs

  def scalaVersion = "2.13.8"
  override def ivyDeps = Agg(
    ivy"com.disneystreaming.smithy4s::smithy4s-aws-kernel:${smithy4sVersion()}",
  )

  /*
ec2, sms, and translate all work (sms is for bulk texting)
sns, and location have compile problems (sns is for sending just a few texts)
 */
  override def smithy4sAwsSpecs = Seq(AwsSpecs.sns)

  def publishVersion = "0.0.0"

  def pomSettings = PomSettings(
    description = "Smithy4sAwsClient",
    organization = "net.walend",
    url = "https://github.com/dwalend/duck-aligner",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("dwalend", "duck-aligner"),
    developers = Seq(Developer("dwalend", "David Walend", "https://github.com/dwalend"))
  )
}