package bleep.scripts.fatjar

import bleep.model.{CrossProjectName, ProjectName}
import bleep.{BleepScript, Commands, PathOps, ProjectPaths, Started, model}
import bleep.packaging.dist
import bloop.config.Config
import org.apache.tools.ant.Project as AntProject
import org.apache.tools.ant.taskdefs.Manifest.Attribute
import org.apache.tools.ant.taskdefs.{Jar as AntJar, Manifest as AntManifest}
import org.apache.tools.ant.types.{FileSet, ZipFileSet}

import java.nio.file.Path
import scala.jdk.CollectionConverters.IteratorHasAsScala

object FatJar extends BleepScript("FatJar") :
  override def run(started: Started, commands: Commands, args: List[String]): Unit =

    val projectName = ProjectName(args.head)
    val crossProjectName: CrossProjectName = model.CrossProjectName(projectName,None)

    val bloopProject: Config.Project = started.bloopFiles(crossProjectName).forceGet.project
    val mainClassName: Option[String] = bloopProject.platform.flatMap(_.mainClass)
    val program = dist.Program(crossProjectName.value, mainClassName.get)

    commands.compile(List(crossProjectName))
    dist(started,crossProjectName,List(program),None)

    val project = started.build.explodedProjects(crossProjectName)
    val projectPaths: ProjectPaths = started.buildPaths.project(crossProjectName, project)
    val distLibDir: Path = projectPaths.targetDir / "dist"

    val destFile = jarPath(started, projectName).toFile
    jarDirectory(started, projectName).toFile.mkdirs()
    destFile.delete()

    val antProject = new AntProject()

    val antJarTask = new AntJar()
    antJarTask.init()
    antJarTask.setProject(antProject)
    antJarTask.setDestFile(destFile)
    antJarTask.setIndex(true)

    val classesPath: Path = started.projectPaths(crossProjectName).classes

    val classesDir = new FileSet()
    classesDir.setProject(antProject)
    classesDir.setDir(classesPath.toFile)
    antJarTask.addFileset(classesDir)

    val libDir = new FileSet()
    libDir.setProject(antProject)
    libDir.setDir(distLibDir.toFile)
    libDir.setIncludes("lib/**")

    libDir.iterator().asScala.foreach{ jar =>
      val jarFileSet = new ZipFileSet()
      jarFileSet.setProject(antProject)
      val file = (distLibDir / jar.getName).toFile
      jarFileSet.setSrc(file)

      antJarTask.addZipfileset(jarFileSet)
    }

    val antManifest = new AntManifest()
    mainClassName.foreach{m => antManifest.addConfiguredAttribute(new Attribute("Main-Class",m))}
    antJarTask.addConfiguredManifest(antManifest)

    antJarTask.execute()

    println(s"Wrote fat jar $destFile with main class $mainClassName")

  def jarPath(started: Started, projectName: ProjectName): Path =
    jarDirectory(started, projectName).resolve(s"${projectName.value}.jar")

  private def jarDirectory(started: Started, projectName: ProjectName): Path =
    started.buildPaths.buildsDir.resolve("normal").resolve(".bloop").resolve(projectName.value).resolve("jars")
