package gin.util;

import java.io.File;
import java.util.List;

import gin.Patch;
import gin.test.UnitTest;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;


/**
 * Method-based GPRuntime search.
 *
 */

public class GPRuntime extends GPSimple {
    
    public static void main(String[] args) {
        GPRuntime sampler = new GPRuntime(args);
        sampler.sampleMethods();
    }   

    public GPRuntime(String[] args) {
        super(args);
    }   

    // Constructor used for testing
    public GPRuntime(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }   

    /*============== Implementation of abstract methods  ==============*/

    protected UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch) {

        UnitTestResultSet results = testPatch(className, tests, origPatch);
        return results;
    }

    // Calculate fitness
    protected long fitness(UnitTestResultSet results) {
    
        return results.totalExecutionTime() / 1000000;
    }   

    // Calculate fitness threshold, for selection to the next generation
    protected boolean fitnessThreshold(UnitTestResultSet results, long orig) {
    
        return results.allTestsSuccessful();
    }   

    // Compare two fitness values, newFitness better if result > 0
    protected long compareFitness(long newFitness, long oldFitness) {

        return oldFitness - newFitness;
    }


}
