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
        System.out.println("Optimising program: " + programName);
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

        System.out.println("Initial execution time:" + bestResult.averageTime);

        for (int i = 0; i < maxEvals; i++) {

            Patch neighbour = neighbour(bestPatch);
            TestRunner.TestResult neighbourResult = tester.test(neighbour);

            System.out.println("Neighbour patch: " + neighbour);
            System.out.println("Neighbour execution time: " + neighbourResult.averageTime);

            // only accept functionally validated solutions
            if (neighbourResult.compiled &&
                    neighbourResult.result.getFailureCount() == 0 &&
                    neighbourResult.averageTime < bestResult.averageTime) {

                bestPatch = neighbour;
                bestResult = neighbourResult;

            }

            System.out.println("Step " + i + " fitness " + bestResult.averageTime);

        }

        return bestPatch;
    }

    public Patch neighbour(Patch patch) {
        return Patch.randomPatch(program, random, maxInitialPatchLength);
    }



}
