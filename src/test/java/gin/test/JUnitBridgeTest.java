package gin.test;

import gin.TestConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class JUnitBridgeTest {

    CacheClassLoader classLoader;

    Object junitBridge;
    Method runnerMethod;

    // Compile source file used by unit tests, found in TestConfiguration.EXAMPLE_DIR_NAME directory.
    private static void buildExampleClasses() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = new ArrayList<>();
            options.add("-cp");
            options.add(System.getProperty("java.class.path"));
            File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
            File exampleFile = new File(resourcesDir, "Error.java");
            File exampleTestFile = new File(resourcesDir, "ErrorTest.java");
            Iterable<? extends JavaFileObject> compilationUnit = fm.getJavaFileObjectsFromFiles(Arrays.asList(
                    exampleFile
                    , exampleTestFile
            ));
            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fm, null, options, null, compilationUnit);
            if (!task.call())
                throw new AssertionError("compilation failed");
        }

    }

    // Instantiate InternalTestRunner by loading via CacheClassLoader
    @Before
    public void setUp() throws Exception {
        classLoader = new CacheClassLoader(TestConfiguration.EXAMPLE_DIR_NAME);
        Class<?> bridgeClass = classLoader.loadClass(JUnitBridge.class.getName());
        junitBridge = bridgeClass.getDeclaredConstructor().newInstance();
        runnerMethod = junitBridge.getClass().getMethod(JUnitBridge.BRIDGE_METHOD_NAME, UnitTest.class, int.class);
        buildExampleClasses();
    }

    @Test
    public void runTestWithException() throws Exception {

        UnitTest test = new UnitTest("ErrorTest", "testException");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;

        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("java.lang.NullPointerException", result.getExceptionType());
        assertTrue(result.getExecutionTime() > 0);
    }

    // Changing timeouts requires class overlaying, so need to use reflection to invoke.
    @Test
    public void timeout() throws Exception {

        // will run for one second, and has a timeout of 10 seconds.
        UnitTest test = new UnitTest("ErrorTest", "testTimeout");

        Object result = null;
        try {
            result = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult res = (UnitTestResult) result;

        assertNotNull(res);
        // So first time: all good
        assertTrue(res.getPassed());
        assertFalse(res.getTimedOut());

        // Now the test should time out
        test.setTimeoutMS(500);

        Object resultWithTimeout = null;
        try {
            resultWithTimeout = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult resTimeout = (UnitTestResult) resultWithTimeout;

        assertNotNull(resTimeout);
        assertFalse(resTimeout.getPassed());
        assertTrue(resTimeout.getTimedOut());

    }

    @Test
    public void assertionError() {
        UnitTest test = new UnitTest("ErrorTest", "testAssertionError");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;
        assertNotNull(result);
        assertFalse(result.getPassed());
    }

    @Test
    public void ignoredTest() {
        UnitTest test = new UnitTest("ErrorTest", "testIgnoredTest");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;

        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals(result.getExecutionTime(), 0);
        assertEquals(result.getCPUTime(), 0);
    }

    @Test
    public void falseAssumption() {
        UnitTest test = new UnitTest("ErrorTest", "testFalseAssumption");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;

        assertNotNull(result);
        assertTrue(result.getPassed());
        assertEquals("org.opentest4j.TestAbortedException", result.getExceptionType());
        assertEquals("Assumption failed: assumption is not true", result.getExceptionMessage());
    }

    @Test
    public void faultyTest() throws Exception {

        UnitTest test = new UnitTest("ErrorTest", "testFaultyTest");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;

        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals(result.getExceptionType(), "java.lang.NullPointerException");
    }

    @Test
    public void incorrectMethodName() throws Exception {

        UnitTest test = new UnitTest("ErrorTest", "thisTestDoesNotExist");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;

        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals("java.lang.NoSuchMethodException", result.getExceptionType());
    }

    @Test
    public void incorrectClassName() throws Exception {

        UnitTest test = new UnitTest("ThisClassDoesNotExist", "testFaultyTest");

        Object resultObj = null;
        try {
            resultObj = runnerMethod.invoke(junitBridge, test, 0);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
        }

        UnitTestResult result = (UnitTestResult) resultObj;

        assertNotNull(result);
        assertFalse(result.getPassed());
        assertEquals(result.getExceptionType(), "java.lang.ClassNotFoundException");
    }

    @After
    public void tearDown() throws Exception {
        File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
        Files.deleteIfExists(new File(resourcesDir, "Error.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ErrorTest.class").toPath());
    }
}
