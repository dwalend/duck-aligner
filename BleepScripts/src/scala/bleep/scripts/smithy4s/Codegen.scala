package bleep.scripts.smithy4s

import bleep.model.CrossProjectName
import bleep.{BleepScript, Commands, Started}

object Codegen extends BleepScript("Smithy4sCodegen") {
  override def run(
                    started: Started,
                    commands: Commands,
                    args: List[String]
                  ): Unit = {

    val projectNameMap = started.globs.exactProjectMap
    val cpn: String =
      args.headOption.getOrElse(sys.error("Must provide a crossProjectName"))
    println(projectNameMap.keySet.mkString(", "))
    val crossProjectName: CrossProjectName = projectNameMap.getOrElse(cpn,
      projectNameMap.getOrElse(s"$cpn@jvm", sys.error(s"'$cpn' is not a valid crossProjectName"))
    )

    commands.clean(List(crossProjectName))

    val theProject = started.buildPaths
      .project(
        crossProjectName,
        started.build.explodedProjects(crossProjectName)
      )

    val sourceDirs = theProject.sourcesDirs
    val fallbackSrcDir = theProject.dir.resolve("src/scala")
    val resourceDir = theProject.dir.resolve("src/resources")

    //todo change this to just part of the compile
    //todo better generated source directory - maybe a smithy template
    //todo is it possible to add a smithy working dir to sourceDirs - via yaml or template or default? - that will be cleaned up with each new build?
    
    val outputDir = sourceDirs.all
      .find(_.toString.endsWith("/shared/src/scala"))
      .orElse(sourceDirs.all.find(_.toString.endsWith("/jvm/src/scala")))
      .orElse(sourceDirs.all.find(_.toString.endsWith("/src/scala")))
      .getOrElse(fallbackSrcDir)

    val smithy4sCodegenPlugin =
      new Smithy4sCodegen(started, crossProjectName, Option(outputDir), Option(resourceDir))

    smithy4sCodegenPlugin.codegen.foreach { file =>
      val message = s"Generated ${file.getAbsolutePath}"
      started.logger.info(message)
    }
  }

}
