package bleep.scripts.scalablytyped

import org.scalablytyped.converter.cli.Main

import bleep.model.{CrossProjectName, ProjectName}
import bleep.{BleepScript, Commands, Started, model}
import bloop.config.Config

object ScalablyTypedGenerator extends BleepScript("stc") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val projectName: ProjectName = ProjectName(args.head)
    val crossProjectName: CrossProjectName = model.CrossProjectName(projectName,None)

    val bloopProject: Config.Project = started.bloopFiles(crossProjectName).forceGet.project

    val sctMain = Main.mainNoExit(Array(
      "--directory",projectName.value,
      "--scala","3.3.7",   //todo from bleep
      "--scalajs","1.20.2",
    ))
