package gin.util;

import java.io.File;
import java.util.List;

import gin.SourceFile;
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

    // Use parent's search strategy
    @Override
    protected void search(String className, List<UnitTest> tests, SourceFile sourceFile) {
        super.search(className, tests, sourceFile);
    }   

    /*============== Implementation of abstract methods  ==============*/

    // Calculate fitness
    protected long fitness(UnitTestResultSet results) {
    
        return results.totalExecutionTime() / 1000000;
    }   

    // Calculate fitness threshold, for selection to the next generation
    protected boolean fitnessThreshold(UnitTestResultSet results, long orig) {
    
        return results.allTestsSuccessful();
    }   


    // Compare two fitness values, result of comparison printed on commandline if > 0
    protected long compareFitness(long newFitness, long best) {

        return best - newFitness;
    }


}
