package bleep.scripts.smithy4s

import bleep.{FileSync, PathOps, Started}
import bleep.model.{CrossProjectName, Dep, IgnoreEvictionErrors, Project, Repository, VersionCombo}
import bleep.packaging.CoordinatesFor
import bleep.packaging.IvyLayout
import bleep.packaging.MapLayout
import bleep.packaging.PackagedLibrary
import bleep.packaging.PublishLayout
import bleep.packaging.packageLibraries
import smithy4s.codegen.CodegenArgs

import java.io.File
import java.nio.file.Path
import scala.collection.SortedMap
import scala.collection.immutable.SortedSet

class Smithy4sCodegen(
                       started: Started,
                       crossProjectName: CrossProjectName,
                       sourcesOutputDir: Option[Path],
                       resourcesOutputDir: Option[Path]
                     ) {
  private val ThisClassName = getClass.getName.split('$').head

  def codegen: Seq[File] = {
    val resolvers = started.build.resolvers.values.collect {
      case Repository.Maven(_, uri) => uri.getRawPath
      case Repository.Ivy(_, uri)   => uri.getRawPath
    }

    val localJars = smithy4sNormalExternalDependencies
      .map(os.Path(_))
    val smithy4sInputs = smithy4sInputDirs
      .map(os.Path(_))
      .filter(os.exists)
      .toList

    val args = CodegenArgs(
      specs = smithy4sInputs,
      output = os.Path(smithy4sOutputDir),
      resourceOutput = os.Path(smithy4sResourceDir),
      skip = Set.empty,
      discoverModels = false,
      allowedNS = smithy4sAllowedNamespaces,
      excludedNS = smithy4sExcludedNamespaces,
      repositories = resolvers,
      dependencies = List.empty,
      transformers = smithy4sModelTransformers,
      localJars = localJars,
      smithyBuild = None,
    )

    val resPaths = smithy4s.codegen.Codegen
      .generateToDisk(args)
      .toList
    resPaths.map(path => new File(path.toString))
  }

  private val project: Project =
    started.build.explodedProjects(crossProjectName)
  private val versionCombo: VersionCombo = VersionCombo
    .fromExplodedProject(project)
    .getOrElse(throw new Exception("No version combo found"))

  private val smithy4sInputDirs: Seq[Path] = Seq(
    started.buildPaths.project(crossProjectName, project).dir.resolve("src/smithy")
  )

  private val smithy4sOutputDir: Path =
    sourcesOutputDir.getOrElse(
      started.buildPaths.generatedSourcesDir(crossProjectName, ThisClassName)
    )

  private val smithy4sResourceDir: Path =
    resourcesOutputDir.getOrElse(
      started.buildPaths.generatedResourcesDir(crossProjectName, ThisClassName)
    )

  private val smithy4sAllowedNamespaces: Option[Set[String]] = Option.empty

  private val smithy4sExcludedNamespaces: Option[Set[String]] = Option.empty

  private def smithy4sNormalExternalDependencies: List[Path] = {
    val transitiveDeps: List[Dep] = started.build
      .transitiveDependenciesFor(crossProjectName)
      .flatMap(_._2.dependencies.values)
      .toList

    val all = project.dependencies.values.toList ++ transitiveDeps

    val packageLibrariesJars = localTemporaryJars(
      "my.org",
      "0.1.0-SNAPSHOT"
    )

    getJars(versionCombo, all) ++ packageLibrariesJars
  }

  private def localTemporaryJars(
                                  groupId: String,
                                  version: String
                                ): Set[Path] = {
    val localProjects =
      started.build.resolvedDependsOn(crossProjectName) ++ Set(crossProjectName)

    val packagedLibraries: SortedMap[CrossProjectName, PackagedLibrary] =
      packageLibraries(
        started,
        coordinatesFor = CoordinatesFor.Default(groupId, version),
        shouldInclude = localProjects,
        publishLayout = PublishLayout.Ivy
      )
    packagedLibraries.flatMap { case (projectName, PackagedLibrary(_, files)) =>
        val jars = files match {
          case IvyLayout(jarFile, _, _, _, _) => MapLayout(Map(jarFile))
          case _ => sys.error("Unsupported publishing layout.")
        }
        val results = FileSync
          .syncBytes(
            started.buildPaths.dotBleepDir / "projects-jars" / crossProjectName.value,
            jars.all,
            deleteUnknowns = FileSync.DeleteUnknowns.No,
            soft = false
          )
        results.log(
          started.logger
            .withContext("projectName",projectName._1.value)
            .withContext("version", version),
          "Published temporary version locally"
        )
        results.keySet
      }.toSet
  }

  private val smithy4sModelTransformers: List[String] = List.empty

  private def getJars(
                       versionCombo: VersionCombo,
                       deps: List[Dep]
                     ): List[Path] =
    started.resolver.force(deps.toSet, versionCombo, SortedSet.empty, "", IgnoreEvictionErrors.No).jars

}