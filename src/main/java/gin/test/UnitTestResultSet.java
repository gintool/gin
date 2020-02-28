package gin.test;

import java.util.ArrayList;
import java.util.List;

//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;

import gin.Patch;

/**
 * Holds the results of running a set of tests.
 * This class holds commonalities between tests relating to patch compilation;
 * the rest of the data is held in a list of UnitTestResult.
 */
public class UnitTestResultSet {

    private List<UnitTestResult> results;

    private Patch patch;
    private boolean patchValid;
    private boolean compiledOK;
    private List<Boolean> editsValid;
    
    /**was the patch effectively a no-op? i.e. was there some difference between
     * input and output source?*/
    private boolean noOp;

    public UnitTestResultSet(Patch patch, boolean patchValid, List<Boolean> editsValid, boolean compiledOK, boolean noOp, List<UnitTestResult> results) {
        this.patch = patch;
        this.patchValid = patchValid;
        this.editsValid = new ArrayList<>(editsValid);
        this.compiledOK = compiledOK;
        this.results = results;
        this.noOp = noOp;
    }

    public Patch getPatch() {
        return patch;
    }

    public boolean getValidPatch() {
        return patchValid;
    }
    
    public List<Boolean> getEditsValid() {
        return editsValid;
    }

    public boolean getCleanCompile() {
        return compiledOK;
    }
    
    public boolean getNoOp() {
        return noOp;
    }

    public List<UnitTestResult> getResults() {
        return results;
    }

    public boolean allTestsSuccessful() {
        for (UnitTestResult testResult : results) {
            if (!testResult.getPassed()) {
                return false;
            }
        }
        return true;
    }

    public long totalExecutionTime() {
        long totalTime = 0;
        for (UnitTestResult testResult : results) {
            totalTime += testResult.getExecutionTime();
        }
        return totalTime;
    }

    ////  Could be used to set timeout for individual tests. Unused at the moment.
    //
    //public Map<UnitTest, long[]> getUnitTestTimes() {
    //    List<UnitTestResult> testResults = this.results.stream().filter(result -> result.getRepNumber() == 0).collect(Collectors.toList());
    //    Map<UnitTest, long[]> testRunTimes = new HashMap<UnitTest, long[]>();
    //    for (UnitTestResult testResult : testResults) {
    //        List<Long> runtimes = new LinkedList<>();
    //        this.results.stream().filter(result -> testResult.getTest() == result.getTest()).forEach(result -> runtimes.add(result.getExecutionTime()));
    //        testRunTimes.put(testResult.getTest(), runtimes.stream().mapToLong(l -> l).toArray());
    //    }
    //    return testRunTimes;
    //}

    @Override
    public String toString() {
        String myrep = String.format("UnitTestResultSet. Patch %s;  Valid: %b; Compiled: %b; NoOp: %b.",
                patch, patchValid, compiledOK, noOp);
        if (results.size() > 0) {
            myrep += " Results follow: ";
        }
        for (UnitTestResult result : results) {
            myrep += " [" + result.toString() + "]";
        }
        return myrep;
    }
}
