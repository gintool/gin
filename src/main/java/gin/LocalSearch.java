package gin;

import java.util.Random;

public class LocalSearch {

    private int seed = 5678;
    private int maxEvals = 100;
    private int maxInitialPatchLength = 3;
    private int WARMUP_REPS = 10;

    private Program program;
    private TestRunner testRunner;
    private Random random;

    /**
     * Main method. Take a source code filename, instantiate a search instance and execute the search.
     * @param args A single source code filename, .java
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please specify a class file to optimise.");
        } else {
            LocalSearch localSearch = new LocalSearch(args[0]);
            Patch result = localSearch.search();
            System.out.println("Best patch found: " + result);
        }

    }

    /**
     * Constructor: Create a program and a testRunner object based on the input filename.
     *              Initialise the RNG.
     * @param programName
     */
    public LocalSearch(String programName) {
        System.out.println("Optimising program: " + programName + "\n");
        this.program = new Program(programName); // just parses the code and counts statements etc.
        this.testRunner = new TestRunner(this.program);
        this.random = new Random(seed);
    }

    /**
     * Actual LocalSearch.
     * @return
     */
    private Patch search() {

        // start with the empty patch
        Patch bestPatch = new Patch(program);
        double bestExecutionTime = calcInitialExecutionTime(bestPatch); // do extra reps

        System.out.println("Initial execution time: " + bestExecutionTime + "\n");

        for (int i = 0; i < maxEvals; i++) {

            Patch neighbour = neighbour(bestPatch);

            System.out.println("Generated Neighbour: " + neighbour);

            TestRunner.TestResult neighbourResult = testRunner.test(neighbour);

            if (!neighbourResult.patchSuccess) {
                System.out.println("Patch invalid");
            } else {
                if (neighbourResult.compiled) {
                    System.out.println("Neighbour Execution Time: " + neighbourResult.executionTime);
                    if (neighbourResult.result.wasSuccessful()) {
                        System.out.println("Passed all tests.");
                    } else {
                        System.out.println("Did not pass all tests.");
                    }
                } else {
                    System.out.println("Failed to compile");
                }

                // only accept functionally validated solutions
                if (neighbourResult.compiled &&
                        neighbourResult.result.getFailureCount() == 0 &&
                        neighbourResult.executionTime < bestExecutionTime) {

                    bestPatch = neighbour;
                    bestExecutionTime = neighbourResult.executionTime;

                }

            }

            System.out.println("Step " + i + " fitness " + bestExecutionTime + "\n");

            bestPatch.writeToFile(program.getFilename() + ".result");


        }

        return bestPatch;
    }

    private double calcInitialExecutionTime(Patch patch) {
        double[] times = new double[WARMUP_REPS];
        for (int i=0; i < WARMUP_REPS; i++) {
            times[i] = testRunner.test(patch).executionTime;
        }
        return TestRunner.median(times);
    }

    /**
     * Generate a neighbouring patch. Currently random choice.
     * @param patch Generate a neighbour of this patch.
     * @return A neighbouring patch.
     */
    public Patch neighbour(Patch patch) {
        return Patch.randomPatch(program, random, maxInitialPatchLength);
    }


}
