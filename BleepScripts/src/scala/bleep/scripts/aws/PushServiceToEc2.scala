package bleep.scripts.aws

import bleep.model.ProjectName
import bleep.scripts.fatjar.FatJar
import bleep.{BleepScript, Commands, Started}

import java.nio.file.Path

object PushServiceToEc2 extends BleepScript("PushServiceToEc2") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =
    //get the server's IP address
    val serverIpAddress = CommonAws.pollForIp

    println(s"serverIpAddresses are $serverIpAddress")

    val projectName = ProjectName(args.head)
    val localFatJar: Path = FatJar.jarPath(started,projectName)
    //scp the latest fat jar

    Scp.scpFile(serverIpAddress,localFatJar,localFatJar.getFileName.toString)
    println(s"scp'ed $localFatJar to $serverIpAddress")
