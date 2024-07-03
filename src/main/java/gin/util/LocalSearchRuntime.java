package gin.util;

import gin.Patch;
import gin.test.UnitTest;
import gin.test.UnitTestResultSet;

import java.io.File;
import java.util.List;

/**
 * @author Giovani
 */
public class LocalSearchRuntime extends LocalSearchSimple {

    public LocalSearchRuntime(String[] args) {
        super(args);
    }

    // Constructor used for testing
    public LocalSearchRuntime(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    public static void main(String[] args) {
        LocalSearchRuntime sampler = new LocalSearchRuntime(args);
        sampler.sampleMethods();
    }

    /*============== Implementation of abstract methods  ==============*/
    protected UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch) {
        return testPatch(className, tests, origPatch, null);
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
