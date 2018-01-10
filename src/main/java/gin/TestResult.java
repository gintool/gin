package gin;
import org.junit.runner.Result;

/**
 * Class to hold the junitResult of running jUnit.
 */
public class TestResult {

    String patchedProgram = "";
    Result junitResult = null;
    double executionTime = -1;
    boolean compiled = false;
    boolean patchSuccess = false;
    public TestResult(Result result, double executionTime, boolean compiled, boolean patchedOK,
                      String patchedProgram) {
        this.junitResult = result;
        this.executionTime = executionTime;
        this.compiled = compiled;
        this.patchSuccess = patchedOK;
        this.patchedProgram = patchedProgram;
    }
    public String toString() {
        boolean junitOK = false;
        if (this.junitResult != null) {
            junitOK = this.junitResult.wasSuccessful();;
        }
        return String.format("Patch Valid: %b; Compiled: %b; Time: %f; Passed: %b", this.patchSuccess,
                this.compiled, this.executionTime, junitOK);
    }

}