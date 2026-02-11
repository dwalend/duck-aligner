package bleep.scripts.scalablytyped

import org.scalablytyped.converter.cli.Main
import bleep.model.{CrossProjectName, ProjectName}
import bleep.{BleepCodegenScript, BleepScript, Commands, Started, model}
import bloop.config.Config

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

object ScalablyTypedGenerator extends BleepCodegenScript("ScalablyTypedGenerator") :

  override def run(started: Started, commands: Commands, targets: List[ScalablyTypedGenerator.Target], args: List[String]): Unit =
    targets.foreach { target =>
      val projectName = target.project.name

      val sctMain: Int = Main.mainNoExit(Array(
        "--directory", projectName.value,
        "--scala", "3.3.7", //todo from bleep
        "--scalajs", "1.20.2", //todo my own package and other command line options
      ))
      val sourcePath = Paths.get(s"${projectName.value}/out")
      val destinationPath = target.sources
      delete(destinationPath.toFile)
      Files.createDirectories(destinationPath)
      Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
    }

  def delete(file: File): Unit =
    if (file.isDirectory)
      file.listFiles.foreach(delete)
    file.delete()
    ()
