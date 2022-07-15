# Apex Types

This provides abstractions for describing types defined in Salesforce's Apex language for use by tooling libraries. The type model is exposed in Scala and can be built for JVM or Node use (via scala.js).

Types are described using an ITypeDeclaration which contains fields for individual parts of the type such as constructors and inner types. For simple elements, such as Modifiers, concrete implementations are provided to ease adoption.     

## Getting Started

### Prerequisites

For OS X follow below, for other OSs setup should be similar but the details will vary.

- Java JDK 1.8

  - OpenJDK installed via brew is recommended
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

## Building

```sh
sbt package
```

This will generate two outputs:

- jvm/target/scala-2.13/apex-types_2.13-X.Y.Z.jar
- js/target/scala-2.13/apex-types_sjs2.13-X.Y.Z.jar

To make these available in your local ivy and maven repositories you can use a target of 'typesJVM' or 'typesJS' combined with either 'publishLocal' for .ivy2 or 'publishM2' for .mvn as needed, e.g. 

```sh
sbt typesJVM/publishM2
```

## Downloading

For Maven use,

```
<dependency>
    <groupId>io.github.apex-dev-tools</groupId>
    <artifactId>apex-types</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

For sbt use, 

```
libraryDependencies += Seq(
      "io.github.apex-dev-tools" %%% "apex-types" % "X.Y.Z"
    )
```
