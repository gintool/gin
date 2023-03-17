package gin.test;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.pmw.tinylog.Logger;

import java.io.Serial;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeoutException;

/**
 * Saves result of a UnitTest run into UnitTestResult.
 * assumes one test case is run through JUnitCore at a time
 * ignored tests and tests with assumption violations are considered successful (following JUnit standard)
 */
public class TestRunListener implements Serializable, TestExecutionListener {

    @Serial
    private static final long serialVersionUID = -1768323084872818847L;
    private static final long MB = 1024 * 1024;
    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final UnitTestResult unitTestResult;
    private long startTime = 0;
    private long startCPUTime = 0;
    private long startMemoryUsage = 0;

    public TestRunListener(UnitTestResult unitTestResult) {
        this.unitTestResult = unitTestResult;
    }


    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            Logger.debug("Test " + testIdentifier.getDisplayName() + " finished.");
            long endTime = System.nanoTime();
            long endCPUTime = threadMXBean.getCurrentThreadCpuTime();
            Runtime runtime = Runtime.getRuntime();
            long endMemoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / MB;
            unitTestResult.setExecutionTime(endTime - startTime);
            unitTestResult.setCPUTime(endCPUTime - startCPUTime);
            unitTestResult.setMemoryUsage(endMemoryUsage - startMemoryUsage);
            Throwable throwable = testExecutionResult.getThrowable().orElse(new RuntimeException("Unknown Exception."));
            switch (testExecutionResult.getStatus()) {
                case FAILED:
                    unitTestResult.setPassed(false);
                    unitTestResult.addFailure(new Failure(Description.createTestDescription("", "", testIdentifier.getUniqueId()),
                            throwable));
                    unitTestResult.setTimedOut(throwable instanceof TimeoutException);
                    break;
                case ABORTED:
                    unitTestResult.addFailure(new Failure(Description.createTestDescription("", "", testIdentifier.getUniqueId()),
                            throwable));
                case SUCCESSFUL:
                    unitTestResult.setPassed(true);
                    break;
            }
        }
    }

    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.isTest()) {
            Logger.debug("Test " + testIdentifier.getDisplayName() + " skipped due to " + reason);
            unitTestResult.setPassed(true);
        }
    }

    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            Logger.debug("Test " + testIdentifier.getDisplayName() + " started.");
            this.startTime = System.nanoTime();
            this.startCPUTime = threadMXBean.getCurrentThreadCpuTime();
            Runtime runtime = Runtime.getRuntime();
            this.startMemoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / MB;
        }
    }

}
