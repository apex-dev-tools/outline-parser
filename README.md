# Apex Outline Parser

This parser extracts an outline from Apex class files. The outline provides structural information about the class and its inner classes without needing to parse code blocks. The performance of this parser is much better than a full parser, making it ideal for use when indexing or similar activities.

If you need access to a full syntax tree for Apex, SOQL or SOSL we recommend using the [apex-parser](https://github.com/apex-dev-tools/apex-parser) instead.

## Getting Started

### Installation

Releases are available from [SonaType](https://s01.oss.sonatype.org). You will need to add the repository to your build tool.

SBT:

  ```scala
  project.settings(
    // Replace %% with %%% to use ScalaJS build
    libraryDependencies += "io.github.apex-dev-tools" %% "outline-parser" % "X.X.X"
  )
  ```

Maven:

  ```xml
  <dependency>
      <groupId>io.github.apex-dev-tools</groupId>
      <artifactId>outline-parser</artifactId>
      <version>X.Y.Z</version>
  </dependency>
  ```

### Usage

The library does not currently provide a documented API. To understand how to use it we recommend looking at the test class `ApexParserCompare.scala` which is used to compare the output with our [full parser](https://github.com/apex-dev-tools/apex-parser) for Apex.

## Development

### Building

The build is a cross project for JS and JVM; SBT commands are aggregated, but can also be executed separately with `sbt parserJVM/[cmd]` or `sbt parserJS/[cmd]`.

Available build commands:

* `sbt package` - Creates packaged jars for testing. e.g. `jvm/target/scala-2.13/outline-parser_2.13-X.Y.Z.jar`
* `sbt pack` / `sbt "pack [version]"` - Do a local published release of the most recent tag or given value.
  * **WARNING:** This can override the remote releases, clear your `~/.ivy2/local` directory to revert.
* `sbt publishLocal` - Same as `pack` except it will generate snapshot versions.
* `sbt test` - Execute full test run.
* `sbt clean` - Removes most build files and artifacts.

### Release

Releases are automated via workflow on publishing a release. Create a `v` prefixed tag at the same time on the commit to be released (e.g. `v1.0.0`).

Snapshot releases can also be created at any time by executing the `Publish` workflow on a branch. The versioning will be in the format `X.X.X+Y-yyyy-SNAPSHOT`; the latest tag followed by recent commit info.
