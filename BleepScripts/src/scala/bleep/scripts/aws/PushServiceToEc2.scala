package bleep.scripts.aws

import bleep.model.ProjectName
import bleep.scripts.fatjar.FatJar
import bleep.{BleepScript, Commands, Started}

import java.nio.file.Path
import scala.jdk.CollectionConverters.ListHasAsScala

object PushServiceToEc2 extends BleepScript("PushServiceToEc2") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

//    commands.script(ScriptName("fat-jar"), args)

    //get the server's IP address
    val serverIpAddress: Seq[String] = CommonAws.describeTestInstances().flatMap(_.networkInterfaces().asScala).map(_.association().publicIp())

    println(s"serverIpAddresses are $serverIpAddress")

    val projectName = ProjectName(args.head)
    val localFatJar: Path = FatJar.jarPath(started,projectName)
    //scp the latest fat jar
    serverIpAddress.foreach { hostname =>
      Scp.scpFile(hostname,localFatJar,localFatJar.getFileName.toString)
      println(s"scp'ed $localFatJar to $hostname")
    }

    //ssh command to stop any old java process

    //ssh command to start the new server
    //java -jar fatjar