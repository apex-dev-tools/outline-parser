import sbtcrossproject.CrossPlugin.autoImport.crossProject

// If you disable this sbt-dynver pulled in by sbt-ci-release will take over
ThisBuild / version := "1.0.0"

inThisBuild(List(
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
  isSnapshot := false,
  scalaVersion := "2.13.3",
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local"  
))

lazy val root = project.in(file(".")).aggregate(parser.js, parser.jvm)

lazy val parser = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .settings(
    name := "outline-parser",
    scalacOptions += "-deprecation",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "io.github.apex-dev-tools" %%% "apex-types" % "1.0.0",
      "org.scalatest" %%% "scalatest" % "3.2.9" % "test"
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
