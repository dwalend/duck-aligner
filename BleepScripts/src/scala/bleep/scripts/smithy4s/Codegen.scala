package bleep.scripts.smithy4s

import bleep.{BleepScript, Commands, Started}

object Codegen extends BleepScript("Smithy4sCodegen") {
  override def run(
                    started: Started,
                    commands: Commands,
                    args: List[String]
                  ): Unit = {

    val projectNameMap = started.globs.exactProjectMap
    val cpn =
      args.headOption.getOrElse(sys.error("Must provide a crossProjectName"))
    val crossProjectName = projectNameMap.getOrElse(cpn, sys.error(s"'$cpn' is not a valid crossProjectName"))
    val theProject = started.buildPaths
      .project(
        crossProjectName,
        started.build.explodedProjects(crossProjectName)
      )

    val sourceDirs = theProject.sourcesDirs
    val fallbackSrcDir = theProject.dir.resolve("src/scala")
    val outputDir = sourceDirs.all
      .find(_.toString.endsWith("/shared/src/scala"))
      .orElse(sourceDirs.all.find(_.toString.endsWith("/jvm/src/scala")))
      .orElse(sourceDirs.all.find(_.toString.endsWith("/src/scala")))
      .getOrElse(fallbackSrcDir)

    val smithy4sCodegenPlugin =
      new Smithy4sCodegen(started, crossProjectName, Some(outputDir), None)

    smithy4sCodegenPlugin.codegen.foreach { file =>
      val message = s"Generated ${file.getAbsolutePath}"
      started.logger.info(message)
    }
  }

}
