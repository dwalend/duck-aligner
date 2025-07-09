package bleep.scripts.aws

import org.apache.tools.ant.Project as AntProject
import org.apache.tools.ant.taskdefs.optional.ssh.Scp as AntScp

import java.nio.file.Path

object Scp {

  def scpFile(hostname:String,fromLocalFile:Path, toRemoteFile:String):Unit = {

    val user: String = "ec2-user"

    val remoteTarget = s"$user@$hostname:$toRemoteFile"

    val antScp = new AntScp()
    antScp.init()
    antScp.setProject(new AntProject())
    antScp.setKeyfile("~/.ssh/davidAtWalendDotNet.pem")
    antScp.setLocalFile(fromLocalFile.toString)
    antScp.setRemoteTofile(remoteTarget)
    antScp.setTrust(true)

    println(s"copying $fromLocalFile to $remoteTarget")
    antScp.execute()

    println(s"copied $fromLocalFile to $remoteTarget")
    println("\u0007")
  }
}

