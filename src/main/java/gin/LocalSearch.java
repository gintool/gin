package gin;

import java.util.Random;

public class LocalSearch {

    private int seed = 5678;
    private int maxEvals = 100;
    private int maxInitialPatchLength = 3;

    private Program program;
    private Tester tester;
    private Random random;

    public LocalSearch(String programName) {
        this.program = new Program(programName);
        this.tester = new Tester(programName, this.program);
        this.random = new Random(seed);
    }

    private Patch search() {

        // start with the empty patch
        Patch bestPatch = new Patch(program);
        Tester.TestResult bestResult = tester.test(bestPatch);
        System.out.println("Initial execution time:" + bestResult.averageTime);

        for (int i = 0; i < maxEvals; i++) {

            Patch neighbour = neighbour(bestPatch);
            Tester.TestResult neighbourResult = tester.test(neighbour);

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

    public static void main(String[] args) {

        if (args.length == 0) {

            System.out.println("Please specify a class file to optimise.");

        } else {

            LocalSearch localSearch = new LocalSearch(args[0]);
            Patch result = localSearch.search();
            System.out.println("Best patch found: " + result);

        }

    }

}
