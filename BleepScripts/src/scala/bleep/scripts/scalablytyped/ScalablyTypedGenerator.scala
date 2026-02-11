package bleep.scripts.scalablytyped

import org.scalablytyped.converter.cli.Main
import bleep.model.{CrossProjectName, ProjectName}
import bleep.{BleepCodegenScript, Commands, Started, model}
import bloop.config.Config

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.annotation.tailrec

object ScalablyTypedGenerator extends BleepCodegenScript("ScalablyTypedGenerator") :

  override def run(started: Started, commands: Commands, targets: List[ScalablyTypedGenerator.Target], args: List[String]): Unit =
    targets.foreach { target =>
      val projectName = target.project.name
      val packageJsonPath = Paths.get(s"${projectName.value}/package.json")

      val sourcePath = Paths.get(s"${projectName.value}/out")
      val destinationPath = target.sources

      def regenerate:Boolean =
        !destinationPath.toFile.exists() ||
        packageJsonPath.toFile.lastModified() > destinationPath.toFile.lastModified()

      if(regenerate)
        val crossProjectName: CrossProjectName = model.CrossProjectName(projectName, None)
        val bloopProject: Config.Project = started.bloopFiles(crossProjectName).forceGet.project
        val scalaVersion =  bloopProject.scala.get.version

        val sctMain: Int = Main.mainNoExit(Array(
          "--directory", projectName.value,
          "--scala", scalaVersion,
          "--scalajs", "1.20.2", //todo from bleep template
          //todo my own package and other command line options
        ))
        delete(destinationPath.toFile)
        Files.createDirectories(destinationPath)
        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
        ()
    }

  def delete(file: File): Unit =
    if (file.isDirectory)
      file.listFiles.foreach(delete)
    file.delete()
    ()
