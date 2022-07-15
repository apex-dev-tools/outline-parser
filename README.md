# Apex Outline Parser

 This parser extracts an outline from Apex class files. The outline provides structural information about the class and its inner classes without needing to parse code blocks. The performance of this parser is much better than a full parser, making it ideal for use when indexing or similar activities.

If you need access to a full syntax tree for Apex, SOQL or SOSL we recommend using the [apex-parser](https://github.com/apex-dev-tools/apex-parser) instead.

## Getting Started

### Prerequisites

- Java JDK 1.8

  - For OS X OpenJDK installed via brew is recommended
    ```sh
    brew tap adoptopenjdk/openjdk
    brew install --cask adoptopenjdk8
    ```
  - For the correct java version to be used, JAVA_HOME must be set accordingly:
    - E.g. To always select JDK 1.8, add the following to your bash/zsh profile
      ```sh
      export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
      ```

- [Scala build tool](https://www.scala-sbt.org/)

```sh
brew install sbt
```

- [Node >= v14](https://nodejs.org/en/) (optional for Javascript testing)


## Building

```sh
sbt package
```

This will generate two outputs:

- jvm/target/scala-2.13/outline-parser_2.13-X.Y.Z.jar
- js/target/scala-2.13/outline-parser_sjs1_2.13-X.Y.Z.jar

These artifacts are best used from Scala code, although the jvm target maybe used with Java albeit with some difficulties mapping types between Java and Scala.

## Usage

The library does not currently provide a documented API. To understand how to use it we recommend looking at the test class ApexParserCompare.scala which is used to compare the output with our [full parser](https://github.com/apex-dev-tools/apex-parser) for Apex.

# Developing

## Intellij

The Scala Intellij plugin has a number of useful features and integrations that make it highly recommended. Ensure you have installed and enabled it first.

The Intellij project files are ignored in this repository, so for a clean repo we need to import and create an sbt project from it.

1. In the IDE, `File > Close Project` if there is an existing project open.
1. Select `Import Project` from the main menu, selecting this repo directory.
1. Then `Import project from external model`, selecting `sbt`.
1. Finally we need to select a JDK version if not already defaulted.
1. You can enable auto import if desired to download dependencies as needed.

After the initial sbt project load you should now be able to start development.

### Building under Windows

Install the following in addition to the requirements above `Git For Windows` and `nvm-windows`.

The build commands documented above work from within a `Git Bash` session.
