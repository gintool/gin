# Gin: Genetic Improvement in No Time

This is the repository for the academic research paper "Genetic Improvement in No Time", submitted to the GI 2017 workshop at GECCO 2017. [Please see this preprint of the paper](doc/gin.pdf).

Gin is a [Genetic Improvement](https://en.wikipedia.org/wiki/Genetic_improvement_(computer_science)) (GI) tool. Genetic Improvement is the application of [Genetic Programming](https://en.wikipedia.org/wiki/Genetic_programming) and other metaheuristics to existing software, to improve it in some way. In its initial incarnation, Gin was designed to reduce the execution time of Java code by modifying source files whilst preserving functionality as embodied by a set of JUnit tests.

This repository was created based on a private development repo and is still undergoing
development. Please raise an issue if you have a specific request or notice a bug.

## The Gin Design Philosophy

The goal of Gin is to stimulate development in GI tooling, and to lower the barrier to experimenting with GI and related ideas such as program fragility.

With this in mind, it was written in Java and targets Java, a language almost univerally familiar to GI researchers. It also mostly avoids functional constructs and similar in Java. In fact, the code is in parts stupidly simple, in that design patterns and good practice are sacrificed in the name of brevity, simplicity, and readability. It is not designed to be an elegant or general solution, but to be easily understood and modified.

In order to reduce Gin's footprint, we rely on the JavaParser and JUnit libraries in order to parse and test the code under optimisation, respectively.

## Getting Started

These instructions will show you how to build gin and run a simple local search on the example program. Gin is designed to be hacked rather than used as a library, so there is no "API" in the traditional sense, you should just modify the code.

### Prerequisites

Gin requires:

* JDK 1.8.x
* Gradle (tested with version 3.3)
* A number of dependencies, which can be downloaded manually or via Gradle (recommended)

JDK downloads:<http://www.oracle.com/technetwork/java/javase/downloads/index.html>

Gradle can be downloaded from their website:<https://gradle.org/install>

The library dependencies can be found in the build.gradle file. Here's a list correct at the time of updating this README:

* [Apache Commons IO 2.5](https://commons.apache.org/proper/commons-io/download_io.cgi)
* [Google Guava 19.0](https://github.com/google/guava/wiki/Release19)
* [JUnit 4.12 and Hamcrest 1.3](https://github.com/junit-team/junit4/wiki/Download-and-Install)
* [JavaParser 3.1.4](https://github.com/javaparser)

### Installing and Building gin

These instructions were tested on OS X.

Clone the repo:

```
git clone https://github.com/drdrwhite/gin
```

Build using gradle (alternatively import into your favourite IDE, such as IntelliJ)

```
cd gin
gradle build
```

If you want to run gin from the commandline, you may want to build a fat jar to avoid having to explicitly include all the depencies in your classpath:

```
gradle shadowJar
```

This will create a fat jar at `build/gin.jar`.

If you want to use gin with an IDE, it can be useful to have all the depencies in a top-level directory:

```
gradle copyToLib
```

This will place all dependencies in the top-level `lib` directory.

## Running a Simple Example

If you build the far jar as above, you can run gin on a simple example with:

```
java -jar build/gin.jar examples/Triangle.java
```

## Analysing Patches

Gin provides a simple tool called PatchAnalyser, which will run a patch specified as a commandline argument and report execution time improvement, as well as dumping out annotated source files indicating statement numbering etc.

## Running Unit Tests

```
gradle test
```

## Contributing

As Gin is still very much in an early stage of development, no process is yet in place for contribution. Please feel free to open issues and submit pull requests. If you'd like to aid in the development of Gin more generally, please get in touch.

## License

This project is licensed under the MIT License. Please see the [LICENSE.md](LICENSE.md) file for details
