import org.scalajs.linker.interface.Report

import scala.sys.process._

ThisBuild / scalaVersion         := "2.13.17"
ThisBuild / description          := "Salesforce Apex outline parser with type definitions"
ThisBuild / organization         := "io.github.apex-dev-tools"
ThisBuild / organizationHomepage := Some(url("https://github.com/apex-dev-tools/outline-parser"))
ThisBuild / homepage             := Some(url("https://github.com/apex-dev-tools/outline-parser"))
ThisBuild / licenses := List(
  "BSD-3-Clause" -> new URL("https://opensource.org/licenses/BSD-3-Clause")
)
ThisBuild / developers := List(
  Developer(
    "apexdevtools",
    "Apex Dev Tools Team",
    "apexdevtools@gmail.com",
    url("https://github.com/apex-dev-tools")
  )
)
ThisBuild / versionScheme := Some("strict")
ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots

// Java 17 development with Java 8 runtime compatibility
ThisBuild / javacOptions ++= Seq("-source", "8", "-target", "8")

lazy val build      = taskKey[File]("Build artifacts")
lazy val pack       = inputKey[Unit]("Publish specific local version")
lazy val npmInstall = taskKey[Unit]("Install Node modules for Scala.js tasks")
lazy val Dev        = config("dev") extend Compile

// Don't publish root
publish / skip := true

// Limit to sequential test for both cross projects
Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)

lazy val parser = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .configs(Dev)
  .settings(
    name := "outline-parser",
    scalacOptions += "-deprecation",
    libraryDependencies ++= Seq(
      "org.scalatest"            %%% "scalatest"  % "3.2.9" % Test,
      "io.github.apex-dev-tools" %%% "apex-ls"    % "6.0.2" % Test
    )
  )
  .jvmSettings(
    build       := buildJVM.value,
    Test / fork := true,
    packageOptions += Package.ManifestAttributes(
      "Class-Path" -> (Compile / dependencyClasspath).value.files.map(_.getName.trim).mkString(" "),
      "Implementation-Build" -> java.time.Instant.now().toEpochMilli.toString
    )
  )
  .jsSettings(
    build                    := buildJs(Compile / fullLinkJS).value,
    Dev / build              := buildJs(Compile / fastLinkJS).value,
    Test / parallelExecution := false,
    npmInstall               := syncNodeModules.value,
    Test / test              := (Test / test).dependsOn(npmInstall).value,
    Test / testOnly          := (Test / testOnly).dependsOn(npmInstall).evaluated,
    Test / testQuick         := (Test / testQuick).dependsOn(npmInstall).evaluated,
    libraryDependencies ++= Seq("net.exoego" %%% "scala-js-nodejs-v14" % "0.12.0"),
    scalaJSUseMainModuleInitializer := false,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    }
  )

lazy val buildJVM = Def.task {
  val targetDir = crossTarget.value
  val targetJar = (Compile / Keys.`package`).value

  // Delete extra jars from target dir
  IO.delete((targetDir * "*.jar").get().filterNot(_.equals(targetJar)))

  // Copy jar deps to target for easier testing
  val files = (Compile / dependencyClasspath).value.files map { f =>
    f -> targetDir / f.getName
  }
  IO.copy(files, CopyOptions().withOverwrite(true))

  targetJar
}

def buildJs(jsTask: TaskKey[Attributed[Report]]): Def.Initialize[Task[File]] = Def.task {
  // Depends on scalaJS fast/full linker output
  jsTask.value

  val targetDir  = crossTarget.value
  val targetFile = (jsTask / scalaJSLinkerOutputDirectory).value / "main.js"

  syncNodeModules.value

  targetFile
}

def syncNodeModules: Def.Initialize[Task[Unit]] = Def.task {
  val log       = streams.value.log
  val npmDir    = baseDirectory.value / "npm"
  val targetDir = crossTarget.value
  val lockFile  = npmDir / "package-lock.json"
  val nodeDir   = npmDir / "node_modules"
  val exec      = run(log)(_, _)
  val needInstall =
    !nodeDir.exists() || (lockFile.exists() && lockFile.lastModified() > nodeDir.lastModified())

  if (needInstall) {
    exec("npm ci", npmDir)
  }

  val packageJson = npmDir / "package.json"
  if (packageJson.exists()) {
    IO.copyFile(packageJson, targetDir / "package.json")
  }
  if (lockFile.exists()) {
    IO.copyFile(lockFile, targetDir / "package-lock.json")
  }

  IO.delete(targetDir / "node_modules")
  if (nodeDir.exists()) {
    IO.copyDirectory(nodeDir, targetDir / "node_modules", CopyOptions().withOverwrite(true))
  } else {
    log.warn("npm node_modules directory not found after installation")
  }
}

// Command to do a local release under a specific version
// Defaults to last reachable tag (ignoring current commit) or 0.0.0
// e.g. sbt "pack 1.2.3-SNAPSHOT" / sbt pack
pack := {
  import sbt.complete.Parsers.spaceDelimited
  val args: Seq[String] = spaceDelimited("<arg>").parsed
  val v                 = args.headOption.getOrElse(previousStableVersion.value.getOrElse("0.0.0"))

  val newState =
    Project.extract(state.value).appendWithoutSession(Seq(ThisBuild / version := v), state.value)
  val proj = Project.extract(newState)

  proj.runTask(parser.jvm / publishLocal, newState)
  proj.runTask(parser.js / publishLocal, newState)
}

// Run a command and log to provided logger
def run(log: ProcessLogger)(cmd: String, cwd: File): Unit = {
  val shell: Seq[String] =
    if (sys.props("os.name").contains("Windows")) Seq("cmd", "/c") else Seq("bash", "-c")
  val exitCode = Process(shell :+ cmd, cwd) ! log
  if (exitCode > 0) {
    log.err(s"Process exited with non-zero exit code: $exitCode")
    sys.exit(exitCode)
  }
}
