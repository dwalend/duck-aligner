package bleep.scripts.scalablytyped

import bleep.model.CrossProjectName
import bleep.{BleepCodegenScript, Commands, Started, model}
import bloop.config.Config as BloopConfig
import org.scalablytyped.converter.cli.Main

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}

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
        //todo add npm install command
        val crossProjectName: CrossProjectName = model.CrossProjectName(projectName, None)
        val bloopProject: BloopConfig.Project = started.bloopFiles(crossProjectName).forceGet.project
        val scalaVersion =  bloopProject.scala.get.version

        //todo maybe take apart the mill plugin instead - or shift away from scalablytyped
        val sctMain: Int = Main.mainNoExit(Array(
          "--directory", projectName.value,
          "--scala", scalaVersion,
          "--scalajs", "1.20.2", //todo from bleep template
          //todo my own package and other command line options
          "--outputPackage", projectName.value,
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
