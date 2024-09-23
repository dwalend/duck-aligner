package bleep.scripts.smithy4s

import bleep.{FileSync, PathOps, Started, commands}
import bleep.model.{CrossProjectName, Dep, Project, Repository, VersionCombo}
import bleep.packaging.CoordinatesFor
import bleep.packaging.IvyLayout
import bleep.packaging.MapLayout
import bleep.packaging.PackagedLibrary
import bleep.packaging.PublishLayout
import bleep.packaging.packageLibraries
import bloop.config.PlatformFiles
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

    val AWS = smithy4s.codegen.AwsSpecs

    def smithy4sAwsSpecs: Seq[String] = {
      Seq("location") //todo get this from somewhere better to lookup aws-location-spec name
    }
    val ivyName = "com.disneystreaming.smithy:aws-location-spec:2023.09.22"

    val classpath: Seq[PlatformFiles.Path] = started.bloopProject(crossProjectName).classpath

    //todo figure out how to use bleep's repository and dependency resolution
    val awsSpecJarPathStrings = List(
      "/Users/dwalend/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/disneystreaming/smithy/aws-location-spec/2023.09.22/aws-location-spec-2023.09.22.jar",
      "/Users/dwalend/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/disneystreaming/smithy/smithytranslate-proto_2.13/0.5.3/smithytranslate-proto_2.13-0.5.3.jar",
      "/Users/dwalend/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/disneystreaming/smithy/smithytranslate-traits/0.5.3/smithytranslate-traits-0.5.3.jar",
      "/Users/dwalend/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/com/disneystreaming/smithy/smithytranslate-transitive_2.13/0.5.3/smithytranslate-transitive_2.13-0.5.3.jar",
    )

    val awsSpecJarPaths: List[os.Path] = awsSpecJarPathStrings.map(os.Path(_))
    //def smithy4sAwsSpecDependencies = s"${AWS.org}:aws-location-spec:${AWS.knownVersion}"

    /*  todo start here when you decide to trim the hair on the Yak
    //mill smith4s plugin code

        def smithy4sAwsSpecDependencies: T[Agg[Dep]] = T {
      val org = AWS.org
      val version = smithy4sAwsSpecsVersion()
      smithy4sAwsSpecs().map { artifactName => ivy"$org:$artifactName:$version" }
    }
        def smithy4sAllExternalDependencies: T[Agg[BoundDep]] = T {
      val bind = bindDependency()
      transitiveIvyDeps() ++
        smithy4sTransitiveIvyDeps().map(bind) ++
        smithy4sExternallyTrackedIvyDeps().map(bind) ++
        smithy4sAwsSpecDependencies().map(bind)
    }
        def smithy4sResolvedAllExternalDependencies: T[Agg[PathRef]] = T {
      resolveDeps(T.task {
        smithy4sAllExternalDependencies()
      })()
    }

    def smithy4sAllDependenciesAsJars: T[Agg[PathRef]] = T {
      smithy4sInternalDependenciesAsJars() ++
        smithy4sResolvedAllExternalDependencies()
    }

       val allLocalJars =
        smithy4sAllDependenciesAsJars()
          .map(_.path)
          .iterator
          .to(List)
       */

    //val localJars = smithy4sNormalExternalDependencies
      //.map(os.Path(_))
   /*
    val localJars = awsSpecJarPaths

    val smithy4sInputs: List[os.Path] = smithy4sInputDirs
      .map(os.Path(_))
      .filter(os.exists)
      .toList
   */
    val smithy4sInputs = List.empty
    val localJars = List.empty

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
      localJars = localJars, //allLocalJars <- smithy4sAllDependenciesAsJars <-
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
    started.buildPaths.project(crossProjectName, project).dir.resolve("smithy")
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
      "walend.net",
      "0.0.0-SNAPSHOT"
    )

    getJars(versionCombo, all) ++ packageLibrariesJars
  }

  private def localTemporaryJars(
                                  groupId: String,
                                  version: String
                                ): Set[Path] = {
    val localProjects =
      started.build.resolvedDependsOn(crossProjectName) ++ Set(crossProjectName)
    val compilation = commands
      .Compile(watch = false, localProjects.toArray)
      .run(started)
    require(
      compilation.isRight,
      s"Compilation of modules required for code generation failed ${crossProjectName.value}."
    )
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
            .withContext(projectName)
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
    started.resolver.force(deps.toSet, versionCombo, SortedSet.empty, "").jars

}