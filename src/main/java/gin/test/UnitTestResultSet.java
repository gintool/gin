package gin.test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

    public UnitTestResultSet(Patch patch, boolean patchValid, boolean compiledOK, List<UnitTestResult> results) {
        this.patch = patch;
        this.patchValid = patchValid;
        this.compiledOK = compiledOK;
        this.results = results;
    }

    public Patch getPatch() {
        return patch;
    }

    public boolean getValidPatch() {
        return patchValid;
    }

    public boolean getCleanCompile() {
        return compiledOK;
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
        String myrep = String.format("UnitTestResultSet. Patch %s;  Valid: %b; Compiled: %b.",
                patch, patchValid, compiledOK);
        if (results.size() > 0) {
            myrep += " Results follow: ";
        }
        for (UnitTestResult result : results) {
            myrep += " [" + result.toString() + "]";
        }
        return myrep;
    }
}
