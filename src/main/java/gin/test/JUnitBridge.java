package gin.test;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pmw.tinylog.Logger;

import java.io.Serial;
import java.io.Serializable;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.Request;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import gin.util.JavaUtils;


// see https://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836
// timeout annotation based on: https://gist.github.com/henrrich/185503f10cbb2499a0dc75ec4c29c8f2 and https://www.baeldung.com/java-reflection-change-annotation-params

/**
 * Runs a given test in the same JVM as this class.
 */
public class JUnitBridge implements Serializable {

    public static final String BRIDGE_METHOD_NAME = "runTest";
    @Serial
    private static final long serialVersionUID = -1984013159496571086L;

    /**
     * This method is called using reflection to ensure tests are run in an environment that employs a separate
     * classloader.
     *
     * @param test the unit test to run
     * @param rep  the number of times to repeat the test
     * @return the test results
     */
    public UnitTestResult runTest(UnitTest test, int rep) {

        JavaUtils.logWorkingDirectoryData("JUnitBridge.runTest");

        UnitTestResult result = new UnitTestResult(test, rep);

        final String className = test.getFullClassName();
        final String methodName = normalizeMethodName(test.getMethodName());

        LauncherDiscoveryRequest request;

        try {
            request = buildRequest(test);

        } catch (ClassNotFoundException e) {
            Logger.error("Unable to find test class file: " + test);
            Logger.error("Is the class file on provided classpath?");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        } catch (NoSuchMethodException e) {
            Logger.error(e.getMessage());
            Logger.error("Note that parameterised JUnit tests are not allowed in Gin.");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        }

        // ensure JUnit Platform uses the same loader that loaded this bridge
        ClassLoader bridgeCL = JUnitBridge.class.getClassLoader();
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(bridgeCL);

        // a few attempts are now made to find the tests and launch them
        // if we can't find them at method level, we run the containing class instead
        // some class level stuff dropped again as we were getting duplicate runs of methods
        // if we still can't find them, we log an error
        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();

            // 1) Discover using the METHOD-scoped request we built
            TestPlan plan = launcher.discover(request);

            // 2) If the engine can’t resolve that method, bail out (or fall back to JUnit4 below)
            if (!plan.containsTests()) {
                // ---- JUnit 4 fallback ----
                Class<?> clazz = Class.forName(test.getFullClassName());
                String method = normalizeMethodName(test.getMethodName());

                if (hasJUnit4(clazz, method)) {
                    Request req = Request.method(clazz, method);
                    Result r = new JUnitCore().run(req);

                    UnitTestResult utr = new UnitTestResult(test, rep);
                    utr.setPassed(r.wasSuccessful());
                    r.getFailures().forEach(f ->
                            utr.addFailure(new Failure(Description.createTestDescription(clazz, method), f.getException()))
                    );

                    if (!r.wasSuccessful() && !r.getFailures().isEmpty()) {
                        Throwable ft = r.getFailures().get(0).getException();
                        utr.setExceptionType(ft == null ? "unknown" : ft.getClass().getName());
                        utr.setExceptionMessage(ft == null ? "" : String.valueOf(ft.getMessage()));
                        utr.setTimedOut(false);
                    }

                    return result;
                } else {
                    result.setPassed(false);
                    result.setExceptionType("gin.test.NoTestsDiscovered");
                    result.setExceptionMessage("No tests discovered for selector "
                            + test.getFullClassName() + "#" + test.getMethodName()
                            + ". Check engine (Jupiter vs Vintage), method name, and classpath.");
                    return result;
                }
            }

            // 3) Execute the SAME request (method-scoped), with the method-scoped listener
            launcher.execute(
                    request,
                    new TestRunListener(
                            result,
                            className,
                            normalizeMethodName(methodName)
                    )
            );

        } catch (Exception e) {
            Logger.error("Error running junit: " + e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }

        return result;

    }

    private LauncherDiscoveryRequest buildRequest(UnitTest test)
            throws ClassNotFoundException, NoSuchMethodException {

        ClassLoader cl = JUnitBridge.class.getClassLoader(); // not getClass().getClassLoader()
        String className = test.getFullClassName();
        String rawMethod = test.getMethodName();

        Class<?> clazz = Class.forName(className, true, cl);
        String method = normalizeMethodName(rawMethod);

        java.lang.reflect.Method m = findMethodDeep(clazz, method);
        if (m == null) throw new NoSuchMethodException(className + "#" + method);

        return org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
                .filters(org.junit.platform.launcher.EngineFilter.includeEngines("junit-jupiter","junit-vintage"))
                .configurationParameter("junit.jupiter.execution.timeout.test.method.default",
                        test.getTimeoutMS() + " ms")
                .selectors(
                        org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod(className, m.getName()),
                        org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(className)
                )
                .build();
    }



    // Normalise method name for selectors:
    //  - strip trailing "()"
    //  - strip parameterized suffixes like "[0]", "[arg=…]"
    private static String normalizeMethodName(String m) {
        String s = m;
        if (s.endsWith("()")) s = s.substring(0, s.length() - 2);
        // remove […] suffixes that appear in parameterized display names
        int idx = s.indexOf('[');
        if (idx > 0) s = s.substring(0, idx);
        return s;
    }

    // Walks superclasses to find a declared method (JUnit 3/4 inheritance cases)
    private static java.lang.reflect.Method findMethodDeep(Class<?> c, String name) {
        Class<?> k = c;
        while (k != null && k != Object.class) {
            for (var m : k.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            k = k.getSuperclass();
        }
        return null;
    }

    private static boolean hasJUnit4(Class<?> clazz, String method) {
        try {
            // walk superclasses to find the method
            for (Class<?> k = clazz; k != null && k != Object.class; k = k.getSuperclass()) {
                for (var mm : k.getDeclaredMethods()) {
                    if (mm.getName().equals(method)) {
                        // JUnit 4?
                        if (mm.isAnnotationPresent(org.junit.Test.class)) return true;
                        // JUnit 3 style?
                        boolean isPublic = java.lang.reflect.Modifier.isPublic(mm.getModifiers());
                        boolean isVoid   = mm.getReturnType() == Void.TYPE;
                        boolean noArgs   = mm.getParameterCount() == 0;
                        if (isPublic && isVoid && noArgs && method.startsWith("test")) return true;
                    }
                }
            }
        } catch (Throwable ignore) {}
        return false;
    }
}
