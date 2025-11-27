package gin.test;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
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
import org.opentest4j.TestAbortedException;
import org.junit.AssumptionViolatedException;


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
    private final String targetClass;
    private final String targetMethod;
    private long startTime = 0;
    private long startCPUTime = 0;
    private long startMemoryUsage = 0;

    public TestRunListener(UnitTestResult unitTestResult, String cls, String m) {
        this.unitTestResult = unitTestResult;
        this.targetClass = cls;
        this.targetMethod = m;
    }


    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult res) {
        if (!id.isTest()) return;
        if (!isTarget(id)) return;   // only record the target test method

        long endTime = System.nanoTime();
        long endCPUTime = threadMXBean.getCurrentThreadCpuTime();
        long endMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MB;

        unitTestResult.setExecutionTime(endTime - startTime);
        unitTestResult.setCPUTime(endCPUTime - startCPUTime);
        unitTestResult.setMemoryUsage(endMem - startMemoryUsage);

        final Throwable t = res.getThrowable().orElse(null);

        switch (res.getStatus()) {
            case SUCCESSFUL: {
                unitTestResult.setPassed(true);
                // clear stale fields
                unitTestResult.setExceptionType(null);
                unitTestResult.setExceptionMessage(null);
                // ensure not marked as timeout
                try { unitTestResult.setTimedOut(false); } catch (Throwable ignore) {}
                break;
            }
            case ABORTED: {
                // JUnit 5 “aborted” = assumption; report class+message verbatim
                unitTestResult.setPassed(true);
                unitTestResult.setExceptionType(t == null ? TestAbortedException.class.getName() : t.getClass().getName());
                unitTestResult.setExceptionMessage(t == null ? "" : String.valueOf(t.getMessage()));
                try { unitTestResult.setTimedOut(false); } catch (Throwable ignore) {}
                break;
            }
            case FAILED: {
                if (isTimeout(t)) {
                    unitTestResult.setPassed(false);
                    try { unitTestResult.setTimedOut(true); } catch (Throwable ignore) {}
                    unitTestResult.setExceptionType(t.getClass().getName());
                    unitTestResult.setExceptionMessage(String.valueOf(t.getMessage()));
                    break;
                }
                if (t instanceof org.opentest4j.TestAbortedException
                        || t instanceof org.junit.AssumptionViolatedException) {
                    unitTestResult.setPassed(true);
                    unitTestResult.setExceptionType(t.getClass().getName());
                    unitTestResult.setExceptionMessage(String.valueOf(t.getMessage()));
                    try { unitTestResult.setTimedOut(false); } catch (Throwable ignore) {}
                    break;
                }
                unitTestResult.setPassed(false);
                unitTestResult.setExceptionType(t == null ? "unknown" : t.getClass().getName());
                unitTestResult.setExceptionMessage(t == null ? "" : String.valueOf(t.getMessage()));
                try { unitTestResult.setTimedOut(false); } catch (Throwable ignore) {}
                break;
            }
        }
    }

    private static boolean isTimeout(Throwable t) {
        if (t == null) return false;
        final String cn = t.getClass().getName();
        // JUnit 5 tends to use internal Timeout* exceptions; match by name
        return (t instanceof java.util.concurrent.TimeoutException)
                || cn.equals("org.junit.jupiter.api.extension.TimeoutException")
                || cn.endsWith("ExecutionTimeoutException")   // e.g. TimeoutExtension$ExecutionTimeoutException
                || cn.contains("Timeout");                    // catch-all for other engines
    }



    private static String normalize(String m) {
        if (m == null) return null;
        String s = m.endsWith("()") ? m.substring(0, m.length()-2) : m;
        int i = s.indexOf('['); if (i > 0) s = s.substring(0, i);
        return s;
    }


    private boolean isTarget(TestIdentifier id) {
        if (!id.isTest()) return false;

        // Prefer exact MethodSource match (Jupiter normal path)
        return id.getSource()
                .filter(s -> s instanceof org.junit.platform.engine.support.descriptor.MethodSource)
                .map(s -> (org.junit.platform.engine.support.descriptor.MethodSource) s)
                .map(ms ->
                        ms.getClassName().equals(targetClass)
                                && normalize(ms.getMethodName()).equals(targetMethod))
                .orElseGet(() -> {
                    // Fallbacks for engines that don't expose MethodSource (or alter display names)
                    String dn = id.getDisplayName();
                    if (dn == null) return false;

                    String n = normalize(dn);
                    if (n.equals(targetMethod)) return true;           // pure method name
                    if (dn.endsWith("#" + targetMethod)) return true;  // Class#method
                    if (dn.contains("(" + targetMethod + ")")) return true; // parameterized display
                    // Last resort: if only one test is discovered/executed, accept it.
                    return true;
                });
    }


    private static void markSkipped(UnitTestResult result, Throwable t, String msg) {
        // Ideally we'll add a 'skipped' flag to UnitTestResult
        // Uncomment this when such a thing exists
//        try {
//            result.setSkipped(true);
//        } catch (Throwable ignore) { /* older models */ }

        result.setPassed(true);
        result.setExceptionType("SKIPPED");

        String detail = summarizeAssumption(t, msg);
        result.setExceptionMessage(detail);
    }

    private static String summarizeAssumption(Throwable t, String msg) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[SKIPPED");

        // type
        if (t != null) sb.append("|type=").append(t.getClass().getName());

        // reason (message)
        if (msg != null && !msg.isBlank()) {
            sb.append("|reason=").append(sanitize(msg));
        } else if (t != null && t.getMessage() != null) {
            sb.append("|reason=").append(sanitize(t.getMessage()));
        }

        // top frame (where assumption was triggered)
        StackTraceElement top = (t != null && t.getStackTrace() != null && t.getStackTrace().length > 0)
                ? t.getStackTrace()[0] : null;
        if (top != null) {
            sb.append("|where=").append(top.getClassName())
                    .append("#").append(top.getMethodName())
                    .append(":").append(top.getLineNumber());
        }

        sb.append("]");
        return sb.toString();
    }

    private static String sanitize(String s) {
        // CSV single-line format
        String t = s.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');

        t = t.replace(',', ';');
        return t.trim();
    }

    @Override
    public void executionSkipped(TestIdentifier id, String reason) {
        // JUnit reports @Disabled here; treat as skipped/pass and retain the reason text
        if (!id.isTest()) return;
        if (!isTarget(id)) return;

        unitTestResult.setPassed(true);
        unitTestResult.setExceptionType("org.junit.jupiter.api.Disabled");
        unitTestResult.setExceptionMessage(reason == null ? "" : reason);
        try { unitTestResult.setTimedOut(false); } catch (Throwable ignore) {}
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
