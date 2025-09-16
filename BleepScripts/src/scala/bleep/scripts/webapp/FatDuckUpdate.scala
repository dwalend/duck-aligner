package bleep.scripts.webapp

import bleep.commands.Link
import bleep.model.{CrossId, CrossProjectName, ProjectName, ScriptName}
import bleep.{BleepScript, Commands, Started, model}

object FatDuckUpdate extends BleepScript("FatDuckUpdate") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val projectName = ProjectName("DuckUpdateService")
    val duckUpdateServiceCrossProjectName: CrossProjectName = model.CrossProjectName(projectName,None)

    //DuckUpdateSmithy generate code and fat jar
    //bleep gen-smithy-code DuckUpdateSmithy
    val smithyProjectName = ProjectName("DuckUpdateSmithy")
    val smithyCrossProjectName = model.CrossProjectName(smithyProjectName,Option(CrossId("js")))

    //compile everything
    commands.compile(List(smithyCrossProjectName))
    commands.compile(List(duckUpdateServiceCrossProjectName))

    //todo generate the MapLibre library

    //DuckUpdateBrowser link release
    val browserProjectName = ProjectName("DuckUpdateBrowser")
    val browserCrossProjectName = model.CrossProjectName(browserProjectName, None)

    val linkBrowser = Link(false,Array(browserCrossProjectName),true)
    val linkBrowserResult = linkBrowser.run(started)
    println(linkBrowserResult)

    //DuckUpdateService fat jar
    commands.script(ScriptName("fat-jar"),List("DuckUpdateService"))
