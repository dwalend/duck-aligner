package bleep.scripts.smithy4s

import bleep.{BleepCodegenScript, Commands, Started}
import ryddig.Logger

object GenerateSmithyScala extends BleepCodegenScript("GenerateResources") {
  override def run(started: Started, commands: Commands, targets: List[GenerateSmithyScala.Target], args: List[String]): Unit = {

    targets.foreach { target =>
      val smithy4sCodegenPlugin = Smithy4sCodegen(started,target.project,Option(target.sources),Option(target.resources))
      val generated = smithy4sCodegenPlugin.codegen
      generated.foreach { file =>
        val message = s"Generated ${file.getAbsolutePath}"
        started.logger.info(message)
      }
    }
  }
}