package gin.util;

import java.io.File;
import java.util.List;

import gin.Patch;
import gin.test.UnitTest;
import gin.test.UnitTestResultSet;


/**
 * Method-based GPMemory search.
 *
 */

public class GPMemory extends GPSimple {
    
    public static void main(String[] args) {
        GPMemory sampler = new GPMemory(args);
        sampler.sampleMethods();
    }   

    public GPMemory(String[] args) {
        super(args);
    }   

    // Constructor used for testing
    public GPMemory(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }   

    /*============== Implementation of abstract methods  ==============*/

    protected UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch) {

        UnitTestResultSet results = testPatch(className, tests, origPatch);
        return results;
    }

    // Calculate fitness
    protected double fitness(UnitTestResultSet results) {
   
        double fitness = Double.MAX_VALUE;
        if (results.getCleanCompile() && results.allTestsSuccessful()) {
            return (double) results.totalMemoryUsage();
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
