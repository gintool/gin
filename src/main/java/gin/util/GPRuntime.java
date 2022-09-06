package gin.util;

import gin.Patch;
import gin.test.UnitTest;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.util.List;


/**
 * Method-based GPRuntime search.
 */

public class GPRuntime extends GPSimple {

    private static final long serialVersionUID = 2553742317029378882L;

    public GPRuntime(String[] args) {
        super(args);
    }

    // Constructor used for testing
    public GPRuntime(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    public static void main(String[] args) {
        GPRuntime sampler = new GPRuntime(args);
        sampler.sampleMethods();
    }

    /*============== Implementation of abstract methods  ==============*/

    protected UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch) {
        UnitTestResultSet results = testPatch(className, tests, origPatch);
        if (!results.allTestsSuccessful()) {
            Logger.error("Original patch does not pass all tests. Cannot continue without a green test suite.");
            UnitTestResult unitTestResult = results.getResults().stream()
                    .filter(uResult -> !uResult.getPassed())
                    .findFirst().get();
            Logger.error("UnitTest: " + unitTestResult.getTest() +
                    "\n\tException: " + unitTestResult.getExceptionMessage() +
                    "\n\tAssertion (E/A): " + unitTestResult.getAssertionExpectedValue() + " / " + unitTestResult.getAssertionActualValue());
            throw new RuntimeException("Failing test suite exception.");
        }
        return results;
    }

    // Calculate fitness
    protected double fitness(UnitTestResultSet results) {

        double fitness = Double.MAX_VALUE;
        if (results.getCleanCompile() && results.allTestsSuccessful()) {
            return (double) (results.totalExecutionTime() / 1000000);
        }
        return fitness;
    }

    // Calculate fitness threshold, for selection to the next generation
    protected boolean fitnessThreshold(UnitTestResultSet results, double orig) {

        return results.allTestsSuccessful();
    }

    // Compare two fitness values, newFitness better if result > 0
    protected double compareFitness(double newFitness, double oldFitness) {

        return oldFitness - newFitness;
    }


}
