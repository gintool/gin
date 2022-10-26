[![Build Status](https://travis-ci.org/gintool/gin.svg?branch=master)](https://travis-ci.org/gintool/gin)

# Gin: A Tool for Experimentation with GI

Gin is a [Genetic Improvement](https://en.wikipedia.org/wiki/Genetic_improvement_(computer_science)) (GI) tool. Genetic Improvement is the application of [Genetic Programming](https://en.wikipedia.org/wiki/Genetic_programming) and other [Metaheuristics](https://en.wikipedia.org/wiki/Metaheuristic) to existing software, to improve it in some way. In its initial incarnation, Gin was designed to reduce the execution time of Java code by modifying source files whilst preserving functionality as embodied by a set of JUnit tests. 

This repository is still undergoing development. Please raise an issue if you have a specific request or notice a bug. If you want to learn more about GI, please see the dedicated [Genetic Improvement website](http://geneticimprovementofsoftware.com/).

Please cite the following papers, if you use Gin for academic purposes: <br>
- release 2 and newer: ["Gin: Genetic Improvement Research Made Easy"](https://github.com/gintool/gin/blob/master/doc/gin2.pdf), Alexander E.I. Brownlee, Justyna Petke, Brad Alexander, Earl T. Barr, Markus Wagner, and David R. White, GECCO Proceedings, 2019. <br>
- release 1: ["GI in No Time"](https://github.com/gintool/gin/blob/master/doc/gin.pdf), David R. White, 3rd International GI Workshop, GECCO Companion Material Proceedings, 2017. 

Extensions:

Please cite the following paper, if using the edits from the [insert](https://github.com/gintool/gin/tree/master/src/main/java/gin/edit/insert) folder:
["Injecting Shortcuts for Faster Running Java Code"](https://ieeexplore.ieee.org/document/9185708), Alexander E. I. Brownlee, Justyna Petke, Anna F. Rasburn, CEC 2020.

Please cite the following paper, if using Regression Test Selection (RTS) strategies:
"Enhancing Genetic Improvement of Software with Regression Test Selection", Giovani Guizzo, Justyna Petke, Federica Sarro, Mark Harman, ICSE 2021.

## The Gin Design Philosophy

The goal of Gin is to stimulate development in GI tooling, and to lower the barrier to experimenting with GI and related ideas such as program fragility. 

With this in mind, it was written in Java and targets Java, a language almost universally familiar to GI researchers. It also mostly avoids functional constructs and similar in Java. In fact, the code is in parts stupidly simple, in that design patterns and good practice are sacrificed in the name of brevity, simplicity, and readability. It is not designed to be an elegant or general solution, but to be easily understood and modified. The second release of Gin largely shares the same philosophy, while providing utilities to handle large Maven and Gradle projects, that bring additional complexity in parts of the codebase.

In order to reduce Gin's footprint, we rely on several Java libraries, e.g., JavaParser and JUnit to parse and test the code under optimisation, respectively. Consequently, operations on statements are done on anything that extends the com.github.javaparser.ast.stmt.Statement class (including blocks).

## Getting Started

These instructions will show you how to build Gin and run a simple local search on the example program. Gin is designed to be hacked rather than used as a library, so there is no "API" in the traditional sense, you should just modify the code.

### Prerequisites

Gin requires:

* JDK 1.8.x  *note: there is currently a known [issue](https://github.com/gintool/gin/issues/29) that prevents Gin running on JDK 9 and above*
* Gradle (tested with version 6.8.2)
* A number of dependencies, which can be downloaded manually or via Gradle (recommended)
* For Maven projects: make sure the Java version is set to 1.8.x

JDK downloads:<http://www.oracle.com/technetwork/java/javase/downloads/index.html>

Gradle can be downloaded from their website:<https://gradle.org/install>

The library dependencies can be found in the build.gradle file.

If you have multiple JREs on your system, you may need to call something like `export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64/jre"` as well as `switch-alternatives` to ensure that Gradle uses Java 8.

### Installing and Building gin

These instructions were tested on OS X and Ubuntu 18.04 LTS.

Clone the repo:

```
git clone https://github.com/gintool/gin.git
```

Build using gradle (alternatively import into your favourite IDE, such as IntelliJ). We also provide a gradle wrapper with Gradle 6.8.2.

```
cd gin
gradle build
```

This will build and test Gin, and also create a fat jar at `build/gin.jar` containing all required dependencies.

If you want to use Gin with an IDE, it can be useful to have all the dependencies in a top-level directory:

```
gradle copyToLib
```

This will place all dependencies in the top-level `lib` directory.

## Running Unit Tests

```
gradle test
```

## Documentation

We provide the javadoc task, though the documentation is yet incomplete.

```
gradle javadoc
```

## Running a Simple Example

If you build the fat jar as above, you can run Gin's local search on a simple example with:

```
java -jar build/gin.jar -f examples/triangle/Triangle.java -m "classifyTriangle(int,int,int)"
```

100 steps will be run by default. Your output will be similar to the following:

```
gin.LocalSearch.search() INFO: Localsearch on file: examples/triangle/Triangle.java method: classifyTriangle(int,int,int)
gin.LocalSearch.search() INFO: Original execution time: 195839357ns
gin.LocalSearch.search() INFO: Step: 1, Patch: | gin.edit.line.CopyLine examples/triangle/Triangle.java:17 -> examples/triangle/Triangle.java:18 |, Failed to compile 
gin.LocalSearch.search() INFO: Step: 2, Patch: | gin.edit.line.CopyLine examples/triangle/Triangle.java:10 -> examples/triangle/Triangle.java:23 |, Time: 395527186ns 
gin.LocalSearch.search() INFO: Step: 3, Patch: | gin.edit.line.DeleteLine examples/triangle/Triangle.java:40 |, New best time: 192528915(ns) 
..
```

The output tells you for each step whether a patch was invalid (e.g. because it could not be applied) or whether the patched code failed to compile or it failed to pass all provided tests. If none of these conditions hold, then the runtime is shown, and it is highlighted if the patch has resulted in the fastest successful passing of all tests seen so far. At the end, a brief summary is shown.


## Logging

We use tinylog for logging, allowing for several logging variants, for example:

```
java -Dtinylog.format="{level}: {message}" -Dtinylog.level=debug -jar build/gin.jar -f examples/triangle/Triangle.java -m "classifyTriangle(int,int,int)"
```

## Utilities

For input arguments to the list of utilities mentioned below, simply run:

```
java -cp build/gin.jar gin.<utility_name>
```

## Analysing Patches

Gin provides a simple tool called PatchAnalyser, which will run a patch specified as a commandline argument and report execution time improvement, as well as dumping out annotated source files indicating statement numbering etc.

```
java -cp build/gin.jar gin.PatchAnalyser -f examples/triangle/Triangle.java -p "| gin.edit.line.DeleteLine examples/triangle/Triangle.java:10 |"
```

NOTE: Due to the way IDs in SourceFileTree are handled, IDs generated against a particular target source file might change from one version of Gin to another. So, don't try to run PatchAnalyser on patches generated by an old version of Gin on a newer version.

## Profiling for Maven and Gradle projects

Gin also provides a Profiler that identifies those parts of the software most exercised by the project's unit tests. 

Before you run the below, please make sure you have Maven installed. The default Maven home path is set to '/usr/local/' (with binary in '/usr/local/bin/mvn'). Please change the path with -mavenHome parameter, if need be. 

```
java -cp build/gin.jar gin.util.Profiler -p my-app -d examples/maven-simple/ -mavenHome <path_to_mavenHome>
```

In case you want to use a Regression Test Selection (RTS) technique to speed up the profiling phase, you can use the `gin.util.RTSProfiler` class instead. RTS is fully supported for Maven projects.

```
java -cp build/gin.jar gin.util.RTSProfiler -p my-app -d examples/maven-simple/ -mavenHome <path_to_mavenHome> -rts ekstazi
```

Gin integrates 3 RTS techniques: [Ekstazi](http://ekstazi.org/) (`-rts ekstazi` - default), [STARTS](https://github.com/TestingResearchIllinois/starts) (`-rts starts`), and a Random selection (`-rts random` - not recommended). STARTS is not supported on Windows. To disable it in `gin.util.RTSProfiler` and use all test cases to test all target methods, use the option `-rts none`.

The output is saved in profiler_output.csv. Note that this is empty for the simple project above as Profiler depends on hprof and inherits its constraints.

We've observed it's best to run Gin from within real-world project's repositories, in case test cases have some unexpected hard-coded dependencies.

A full example with an existing Maven project is given further below.

## Automated test case generation for Maven and Gradle projects

Gin uses [EvoSuite](http://www.evosuite.org/) to generate test cases automatically. Make sure test class file are available (all constraints of EvoSuite are inherited).

Before you run the below, please make sure you have Maven installed. The default Maven home path is set to '/usr/local/' (with binary in '/usr/local/bin/mvn'). Please change the path with -mavenHome parameter, if need be. 

```
java -cp build/gin.jar gin.util.TestCaseGenerator -d examples/maven-simple -p my-app -classNames com.mycompany.app.App -generateTests -mavenHome <path_to_mavenHome>
```

Generated tests can be run and integrated with Profiler, PatchAnalyser and Samplers for Maven projects only. Gradle support is in development, though the generated tests can be run if EvoSuite dependency is added to the build.gradle file of the project (please see an example init.gradle file in the testgeneration directory). 

Note that the below command will instrument the pom.xml file with the EvoSuite dependency. If invoking Samplers with the generated tests, please supply the EvoSuite dependency on the classpath, e.g., `java -cp build/gin.jar:testgeneration/evosuite-1.0.6.jar gin.<utility>'.

```
java -cp build/gin.jar gin.util.TestCaseGenerator -projectDir examples/maven-simple -projectName my-app -test -mavenHome <path_to_mavenHome>
```

You can also remove all tests from the project's directory before new test generation:

```
java -cp build/gin.jar gin.util.TestCaseGenerator -d examples/maven-simple -p my-app -removeTests
```


## Samplers

We provide an abstract Sampler class that provides utilities for testing patches made at the class and method level. It takes method names and tests associated with those methods as input. Please note that you can simply supply the output of Profiler as an input file. Also note that we capture both the actual and expected result in case of failed assertions. Also worth noting that the samplers will probably produce different lists of patches with different versions of Gin (owing to updates to Java syntax, the RNG, and supporting libraries that mean the space of possible edits changes).

We provide three subclasses to show example usage scenarios and show how easily Gin can be extended.

EmptyPatchTester simply runs all the project tests (declared in the input method file) through Gin:

```
java -cp build/gin.jar gin.util.EmptyPatchTester -d examples/triangle/ -c examples/triangle/ -m examples/triangle/method_file.csv
```

DeleteEnumerator deletes each line and each statement in a method (sampled at random, without replacement) and saves the result to sampler_output.csv. 

```
java -cp build/gin.jar gin.util.DeleteEnumerator -d examples/triangle/ -c examples/triangle/ -m examples/triangle/method_file.csv
```

RandomSampler makes random edits to a project and saves the results to sampler_output.csv. 

```
java -cp build/gin.jar gin.util.RandomSampler -d examples/triangle/ -c examples/triangle/ -m examples/triangle/method_file.csv
```

Assuming EvoSuite tests were generated and original tests not removed:

```
java -cp build/gin.jar:testgeneration/evosuite-1.0.6.jar gin.util.RandomSampler -d examples/maven-simple -p my-app -m examples/maven-simple/example_profiler_results.csv -mavenHome <path_to_mavenHome>
```

Gin also offers an implementation of the multi-objective algorithm NSGA-II for improving the execution time and memory consumption of software.
You can run it similarly to the other samplers shown above, for the example triangle project the following command should be called:

```
java -cp build/gin.jar gin.algorithm.nsgaii.NSGAII -d examples/triangle/ -c examples/triangle/ -m examples/triangle/method_file.csv
```

## Test Runners

The tests can be run internally (through InternalTestRunner), or in a separate jvm (through ExternalTestRunner). Moreover, each test case can be run in a separate jvm as well. This covers the situation where a target project has multiple threads that mean tests cannot be run safely in parallel. We added these options to Samplers, for example:

Tests run in a separate jvm:
```
java -cp build/gin.jar gin.util.EmptyPatchTester -d examples/triangle/ -c examples/triangle/ -m examples/triangle/method_file.csv -j
```

Each test case run in a separate jvm:
```
java -cp build/gin.jar gin.util.EmptyPatchTester -d examples/triangle/ -c examples/triangle/ -m examples/triangle/method_file.csv -J
```
## Full Example with a Maven Project

We will now try cloning, profiling, and sampling for a project taken from GitHub: spatial4j.

First, move into the examples directory for working, and clone the project.
```
cd examples
git clone https://github.com/locationtech/spatial4j.git
cd spatial4j
git checkout tags/spatial4j-0.7
```

Build with maven to ensure we have all the dependencies we need, that the original java source has compiled, and that the tests all run:
```
mvn compile
mvn test
```

Run Gin's Profiler. In this case we limit to the first 20 tests for speed using `-n 20`. Results are written to a CSV. This CSV is used to specify the target methods and unit tests for the samplers below.
```
projectnameforgin='spatial4j'; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.Profiler -r 1 -mavenHome /usr/share/maven -n 20 -p $projectnameforgin -d . -o $projectnameforgin.Profiler_output.csv
```

Run EmptyPatchTester. This serves as a baseline, showing the performance of the original unaltered code against the unit tests.
```
projectnameforgin='spatial4j'; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.EmptyPatchTester -J -p $projectnameforgin -d . -m $projectnameforgin.Profiler_output.csv -o $projectnameforgin.EmptyPatchTester_output.csv  -mavenHome /usr/share/maven
```

Run RandomSampler to test the effect of different edits in the space. Here, we limit to statement edits; we allow only 1 edit per patch; and we test 100 edits sampled at random.
```
projectnameforgin='spatial4j'; editType='STATEMENT'; patchSize='1'; patchNumber='100'; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.RandomSampler -j -p $projectnameforgin -d . -m $projectnameforgin.Profiler_output.csv -o $projectnameforgin.RandomSampler_${editType}_patchSize${patchSize}_patchNumber${patchNumber}_output.csv -mavenHome /usr/share/maven -editType $editType -patchNumber $patchNumber -patchSize $patchSize
```


## Contributing

Please feel free to open issues and submit pull requests. If you'd like to aid in the development of Gin more generally, please get in touch with [Sandy Brownlee](mailto:sbr@cs.stir.ac.uk) or [Justyna Petke](mailto:j.petke@ucl.ac.uk). 

## License

This project is licensed under the MIT License. Please see the [LICENSE.md](LICENSE.md) file for details. By submitting a pull request, you agree to license your contribution under the MIT license to this project.
