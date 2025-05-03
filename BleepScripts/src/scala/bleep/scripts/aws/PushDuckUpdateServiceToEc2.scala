package bleep.scripts.aws

import bleep.model.ScriptName
import bleep.{BleepScript, Commands, Started}

object PushDuckUpdateServiceToEc2 extends BleepScript("PushDuckUpdateServiceToEc2") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =
    val duckServerIp = CommonAws.pollForIp
    Ssh.runCommand(duckServerIp,"systemctl --user stop DuckUpdate")

    commands.script(ScriptName("fat-duck-update"),List.empty)
    commands.script(ScriptName("push-to-ec2"),List("DuckUpdateService"))

    Ssh.runCommand(duckServerIp, "systemctl --user start DuckUpdate")
