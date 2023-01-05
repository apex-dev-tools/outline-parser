inThisBuild(
  List(
    description := "Apex outline parser",
    organization := "io.github.apex-dev-tools",
    homepage := Some(url("https://github.com/apex-dev-tools/outline-parser")),
    licenses := List("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")),
    developers := List(
      Developer(
        "apexdevtools",
        "Apex Dev Tools Team",
        "apexdevtools@gmail.com",
        url("https://github.com/apex-dev-tools")
      )
    ),
    versionScheme := Some("strict"),
    scalaVersion := "2.13.10",
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
  )
)

lazy val pack = inputKey[Unit]("Publish specific local version")

// Don't publish root
publish / skip := true

lazy val parser = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    name := "outline-parser",
    scalacOptions += "-deprecation",
    libraryDependencies ++= Seq(
      "io.github.apex-dev-tools" %%% "apex-types" % "1.2.0",
      "org.scalatest"            %%% "scalatest"  % "3.2.9" % "test"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.github.nawforce"      % "uber-apex-jorje" % "1.0.0" % Test,
      "io.github.apex-dev-tools" % "apex-parser"     % "3.0.0" % Test
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := false,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    }
  )

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
