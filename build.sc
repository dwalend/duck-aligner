import $ivy.`com.disneystreaming.smithy4s::smithy4s-mill-codegen-plugin::0.18.24`
import smithy4s.codegen.mill._

import mill._, mill.scalalib._
object Smithy4sAwsClient extends ScalaModule with Smithy4sModule {
  import smithy4s.codegen.AwsSpecs

  def scalaVersion = "2.13.8"
  override def ivyDeps = Agg(
 //   ivy"com.disneystreaming.smithy4s::smithy4s-core:${smithy4sVersion()}",
 //   ivy"com.disneystreaming.smithy4s::smithy4s-http4s:${smithy4sVersion()}",
 //   ivy"com.disneystreaming.smithy4s::smithy4s-aws-http4s:${smithy4sVersion()}",
 //   ivy"org.http4s::http4s-ember-client:0.23.26"
    ivy"com.disneystreaming.smithy4s::smithy4s-aws-kernel:${smithy4sVersion()}",
  )

  override def smithy4sAwsSpecs = Seq(AwsSpecs.sms)//Seq(AwsSpecs.location)
//  override def smithy4sAwsSpecs = Seq(AwsSpecs.translate)
}