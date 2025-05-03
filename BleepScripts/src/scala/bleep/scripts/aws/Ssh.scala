package bleep.scripts.aws

import org.apache.tools.ant.Project as AntProject
import org.apache.tools.ant.taskdefs.optional.ssh.SSHExec as AntSsh

object Ssh {

  def runScript(hostname:String, scriptFileName:String):Unit = {
    val antSsh = createAntSsh(hostname)
    antSsh.setCommandResource(scriptFileName)

    println(s"executing $scriptFileName on $hostname")
    antSsh.execute()

    println(s"executed $scriptFileName on $hostname")
  }

  def runCommand(hostname:String, command:String):Unit = {
    val antSsh = createAntSsh(hostname)
    antSsh.setCommand(command)

    println(s"executing $command on $hostname")
    antSsh.execute()

    println(s"executed $command on $hostname")
  }
  
  private def createAntSsh(hostname:String) = {
    val antSsh = new AntSsh()
    antSsh.init()
    antSsh.setProject(new AntProject())
    antSsh.setKeyfile("~/.ssh/davidAtWalendDotNet.pem")
    antSsh.setTrust(true)

    antSsh.setHost(hostname)
    antSsh.setUsername("ec2-user")  
    
    antSsh
  }
}

