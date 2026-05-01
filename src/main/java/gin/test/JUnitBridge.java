package gin.test;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pmw.tinylog.Logger;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

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

        UnitTestResult result = new UnitTestResult(test, rep);

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
            Logger.error("Note that parametirised JUnit tetsts are not allowed in Gin.");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.error("Exception when instrumenting tests with a timeout: " + e);
            Logger.error(e.getMessage());
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        }

        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            TestPlan testPlan = launcher.discover(request);
            launcher.execute(testPlan, new TestRunListener(result));
        } catch (Exception e) {
            Logger.error("Error running junit: " + e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        return result;

    }

    public LauncherDiscoveryRequest buildRequest(UnitTest test) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        ClassLoader loader = this.getClass().getClassLoader();

        String testClassname = test.getFullClassName();
        Class<?> clazz = loader.loadClass(testClassname);

        String methodName = test.getMethodName().replace("()", "");
        Method method = clazz.getDeclaredMethod(methodName);

        return LauncherDiscoveryRequestBuilder.request()
                .selectors(selectMethod(clazz, method.getName()))
                .configurationParameter("junit.jupiter.execution.timeout.test.method.default", test.getTimeoutMS() + " ms")
                .build();
    }
}
