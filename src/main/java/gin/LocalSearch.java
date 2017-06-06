package gin;

import java.util.Random;

public class LocalSearch {

    private int seed = 5678;
    private int maxEvals = 100;
    private int maxInitialPatchLength = 3;

    private Program program;
    private TestRunner tester;
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
     * Constructor: Create a program and a tester object based on the input filename.
     *              Initialise the RNG.
     * @param programName
     */
    public LocalSearch(String programName) {
        System.out.println("Optimising program: " + programName + "\n");
        this.program = new Program(programName); // just parses the code and counts statements etc.
        this.tester = new TestRunner(this.program);
        this.random = new Random(seed);
    }

    /**
     * Actual LocalSearch.
     * @return
     */
    private Patch search() {

        // start with the empty patch
        Patch bestPatch = new Patch(program);
        TestRunner.TestResult bestResult = tester.test(bestPatch);

        System.out.println("Initial execution time: " + bestResult.averageTime + "\n");

        for (int i = 0; i < maxEvals; i++) {

            Patch neighbour = neighbour(bestPatch);

            System.out.println("Generated Neighbour: " + neighbour);

            TestRunner.TestResult neighbourResult = tester.test(neighbour);

            if (neighbourResult.compiled) {
                System.out.println("Neighbour Execution Time: " + neighbourResult.averageTime);
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
                    neighbourResult.averageTime < bestResult.averageTime) {

                bestPatch = neighbour;
                bestResult = neighbourResult;

            }

            System.out.println("Step " + i + " fitness " + bestResult.averageTime + "\n");

        }

        return bestPatch;
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
