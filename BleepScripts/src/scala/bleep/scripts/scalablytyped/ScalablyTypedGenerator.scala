package bleep.scripts.scalablytyped

import org.scalablytyped.converter.cli.Main
import bleep.model.CrossProjectName
import bleep.{BleepCodegenScript, Commands, Started, model}
import bloop.config.Config as BloopConfig
import com.olvind.logging.LogLevel
import fansi.Color
import org.scalablytyped.converter.cli.Main.{Config, DefaultConfig, ParseConversionOptions, logger, parseCachePath, table}
import org.scalablytyped.converter.internal.importer.build.{PublishedSbtProject, SbtProject, ScalaCliCompiler}
import org.scalablytyped.converter.internal.importer.documentation.Npmjs
import org.scalablytyped.converter.internal.importer.{Bootstrap, LibScalaJs, LibTsSource, PersistingParser, Phase1ReadTypescript, Phase2ToScalaJs, Phase3Compile, PhaseFlavour}
import org.scalablytyped.converter.internal.phases.PhaseListener.NoListener
import org.scalablytyped.converter.internal.phases.{PhaseRes, PhaseRunner, RecPhase}
import org.scalablytyped.converter.internal.scalajs.Name
import org.scalablytyped.converter.internal.ts.CalculateLibraryVersion.PackageJsonOnly
import org.scalablytyped.converter.internal.ts.{PackageJson, TsIdentLibrary}
import org.scalablytyped.converter.internal.{InFolder, Json, constants, files, sets}
import scopt.OParser
import sourcecode.Text.generate

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.collection.immutable.SortedSet
import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

        //todo take apart the mill plugin instead
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
