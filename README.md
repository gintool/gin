# Gin LLM User Guide

This guide provides instructions on how to recreate the experiments detailed in the paper **"Large Language Model-based Code Completion is an Effective Genetic Improvement Mutation Operator."** It focuses solely on the experiments; for more information about GIN, please refer to the [README](https://github.com/Andydiantu/gin/blob/llm_combined_exp/README_GIN.md).

## Code Overview

Below is an explanation of the functionality related to the LLM-enhanced genetic improvement (GI) mutation operations.

### Algorithms

- `src/main/java/gin/util/RandomSampler.java`: Implements the random sampling algorithms.
- `src/main/java/gin/util/LocalSearchRuntime.java`: Implements the local search algorithms (hill climber used in this paper).

### LLM Utilities

- `src/main/java/gin/edit/llm/LLMConfig.java`: Contains all LLM prompts and environment configurations for interacting with large language models.
- `src/main/java/gin/edit/llm/Ollama4jLLMQuery.java`: Implements the interface for querying a locally hosted Ollama server.
- `src/main/java/gin/edit/llm/OpenAILLMQuery.java`: Implements the interface for querying a remote OpenAI server.

### LLM-Enhanced Mutation Operators

- `src/main/java/gin/edit/llm/LLMReplaceStatement.java`: Implements the LLM-based replacement mutation operator, as proposed by Brownlee et al.
- `src/main/java/gin/edit/llm/LLMMaskedStatement.java`: Implements the LLM-based masking mutation operator, proposed in this paper.

## Getting Started

### Prerequisites

To set up the environment, ensure the following software is installed:

1. **JDK 17**: Download from [Oracle Java Downloads](https://www.oracle.com/java/technologies/downloads/?er=221886).
2. **Gradle**: Version 8.0.2 or higher. Install from [Gradle](https://gradle.org/install).
3. **Ollama**: Version 0.1.48 or higher. Download from [Ollama](https://ollama.com/download).

### Download and Install GIN

1. Clone the GIN repository:

    ```bash
    git clone https://github.com/Andydiantu/gin.git
    ```

2. Check out the LLM branch:

    ```bash
    git checkout llm_combined_exp
    ```

3. Build the project and install dependencies by running:

    ```bash
    gradle build
    ```

### Setting Up LLM

The paper experiments with five LLMs, detailed in the table below:

| Name         | ID             | Quantisation | Parameters | Size   |
|--------------|----------------|--------------|------------|--------|
| Gemma2 2B    | 430ed3535049   | Q4_0         | 2.61B      | 1.7 GB |
| Gemma2 9B    | ff02c3702f32   | Q4_0         | 9.24B      | 5.4 GB |
| Llama3.1 8B  | 91ab477bec9d   | Q4_0         | 8.03B      | 4.7 GB |
| Mistral 7B   | 61e88e884507   | Q4_0         | 7.24B      | 4.1 GB |
| Phi3 14B     | cf611a26b048   | Q4_0         | 14.0B      | 7.9 GB |

To experiment with the **Gemma2:2b** model:

1. Download the model with the following command:

    ```bash
    ollama run gemma2:2b
    ```

### Setting Up the Benchmark

The experiments use five benchmark projects, detailed below:

| Project       | URL                                              | Branch                |
| ------------- | ------------------------------------------------ | --------------------- |
| JCodec        | [github.com/jcodec/jcodec](https://github.com/jcodec/jcodec) | master (7e52834)      |
| JUnit4        | [github.com/junit-team/junit4](https://github.com/junit-team/junit4) | r4.13.2               |
| Gson          | [github.com/google/gson](https://github.com/google/gson) | gson-parent-2.10.1    |
| Commons-Net   | [github.com/apache/commons-net](https://github.com/apache/commons-net) | commons-net-3.10.0    |
| Karate        | [github.com/karatelabs/karate](https://github.com/karatelabs/karate) | v1.4.1                |

To run an experiment using **JCodec** and **Gemma2:2b**:

1. Navigate to the examples folder in the GIN project:

    ```bash
    cd examples
    ```

2. Clone the JCodec project:

    ```bash
    git clone https://github.com/jcodec/jcodec.git
    ```

3. Build the JCodec project:

    ```bash
    mvn build
    ```

4. Profile the project:

    ```bash
    projectnameforgin='jcodec'; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.Profiler -r 20 -mavenHome /PATH/TO/MAVEN -p $projectnameforgin -d . -o $projectnameforgin.Profiler_output.csv
    ```

    Replace `/PATH/TO/MAVEN` with your Maven home directory.

### Running Experiments

To run the experiments described in the paper, use the following commands:

#### Masking Random Search
```bash
projectnameforgin='jcodec'; LLM = 'gemma2:2b'; model=$LLM; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.RandomSampler -j -p $projectnameforgin -d . -m $projectnameforgin.Profiler_output.csv -o results/$projectnameforgin.RandomSampler_1000_output.$model.csv -mavenHome /PATH/TO/MAVEN -timeoutMS 10000 -et gin.edit.llm.LLMMaskedStatement -mt $model -pt MASKED -pn 1000 &> results/$projectnameforgin.RandomSampler_COMBINED_1000_stderrstdout_.$model.txt`
```
#### Masking Local Search

```bash
projectnameforgin='jcodec'; LLM = 'gemma2:2b'; model=$LLM; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.LocalSearchRuntime -j -p $projectnameforgin -d . -m $projectnameforgin.Profiler_output.csv -o results/$projectnameforgin.LocalSearchRuntime_COMBINED_50_output.$model.csv -mavenHome /PATH/TO/MAVEN -timeoutMS 10000 -et gin.edit.llm.LLMMaskedStatement -mt $LLM -pt MASKED -in 100 &> results/$projectnameforgin.LocalSearchRuntime_LLM_MASKED_50_stderrstdout.$model.txt`
```
#### Combined Random Search with 70% probability of choosing masking mutation
```bash
projectnameforgin='jcodec'; LLM = 'gemma2:2b'; model=$LLM; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.RandomSampler -j -pb 0.7 -p $projectnameforgin -d . -m $projectnameforgin.Profiler_output.csv -o results/$projectnameforgin.RandomSampler_COMBINED_1000_output.$model.csv -mavenHome /PATH/TO/MAVEN -timeoutMS 10000 -et gin.edit.llm.LLMMaskedStatement,STATEMENT -mt $model -pt MASKED -pn 100 &> results/$projectnameforgin.RandomSampler_COMBINED_1000_stderrstdout_.$model.txt`
```
#### Combined Local Search with 70% probability of choosing masking mutation
```bash
projectnameforgin='jcodec';  LLM = 'gemma2:2b'; model=$LLM; java -Dtinylog.level=trace -cp ../../build/gin.jar gin.util.LocalSearchRuntime -j -pb 0.7 -p $projectnameforgin -d . -m $projectnameforgin.Profiler_output.csv -o results/$projectnameforgin.LocalSearchRuntime_COMBINED_50_output.$model.csv -mavenHome /PATH/TO/MAVEN -timeoutMS 10000 -et gin.edit.llm.LLMMaskedStatement,STATEMENT -mt $LLM -pt MASKED -in 100 &> results/$projectnameforgin.LocalSearchRuntime_LLM_MASKED_50_stderrstdout.$model.txt`
```

(NOTE: Replace the /PATH/TO/MAVEN with your maven home directory).
