package gin.test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.pmw.tinylog.Logger;

/**
 * Saves result of a UnitTest run into UnitTestResult.
 * assumes one test case is run through JUnitCore at a time
 * ignored tests and tests with assumption violations are considered successful (following JUnit standard)
 */
public class TestRunListener extends RunListener {

    private static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    private UnitTestResult unitTestResult;

    private long startTime = 0;

    private long startCPUTime = 0;

    public TestRunListener(UnitTestResult unitTestResult) {
        this.unitTestResult = unitTestResult;
    }

    public void testAssumptionFailure(Failure failure) {
        Logger.debug("Test " + failure.getTestHeader() + " violated an assumption. Skipped.");
        unitTestResult.addFailure(failure);
    }

    public void testFailure(Failure failure) throws Exception {
        Logger.debug("Test " + failure.getTestHeader() + " produced a failure.");
        unitTestResult.addFailure(failure);
    }

    public void testFinished(Description description) throws Exception {
        Logger.debug("Test " + description + " finished.");
        long endTime = System.nanoTime();
        long endCPUTime = threadMXBean.getCurrentThreadCpuTime();
        unitTestResult.setExecutionTime(endTime - startTime);
        unitTestResult.setCPUTime(endCPUTime - startCPUTime);
    }

    public void testIgnored(Description description) throws Exception {
        Logger.debug("Test " + description + " ignored.");
    }

    public void testRunFinished(Result result) throws Exception {
        if (result.wasSuccessful()) {
            unitTestResult.setPassed(true);
        }
    }

    public void testRunStarted(Description description) throws Exception {
        assert(description.testCount() == 1);
    }

    public void testStarted(Description description) throws Exception {
        Logger.debug("Test " + description + " started.");
        this.startTime = System.nanoTime();
        this.startCPUTime = threadMXBean.getCurrentThreadCpuTime();
    }

}
