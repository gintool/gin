package gin.test;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Result;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class IsolatedTestRunnerTest {

    IsolatedTestRunner runner;
    File RESOURCES_DIR = new File("./src/test/resources/");
    File SRC_FILE = new File(RESOURCES_DIR, "ExampleTriangleProgram.java");

    @Test
    public void runTestClasses() throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {

        String expectedSrc= null;
        try {
            Charset charSet = null; // platform default
            expectedSrc = FileUtils.readFileToString(SRC_FILE, charSet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(expectedSrc);

        ClassLoader classLoader = new CacheClassLoader(RESOURCES_DIR);
        classLoader.loadClass("ExampleTriangleProgramTest");

        Class runnerClass = classLoader.loadClass(IsolatedTestRunner.class.getName());

        Object runner = runnerClass.newInstance();

        Method method = runner.getClass().getMethod("runTestClasses", List.class);

        List<String> testClasses = new LinkedList<>();
        testClasses.add("ExampleTriangleProgramTest");

        Object result = method.invoke(runner, testClasses);

        // Due to class loader issues as we're running in junit (and it has a separate classloader)
        // we need to use reflection rather than casting the result.
        Method getExecutionTime = result.getClass().getMethod("getExecutionTime");
        Double executionTime = (Double)getExecutionTime.invoke(result);

        Method getCleanCompile = result.getClass().getMethod("getCleanCompile");
        boolean cleanCompile = (boolean)getCleanCompile.invoke(result);

        Method getValidPatch = result.getClass().getMethod("getValidPatch");
        boolean validPatch = (boolean)getValidPatch.invoke(result);

        Method getJunitResult = result.getClass().getMethod("getJunitResult");
        Object junitResult =  getJunitResult.invoke(result);
        Method getFailureCount = junitResult.getClass().getMethod("getFailureCount");
        int failureCount = (int)getFailureCount.invoke(junitResult);

        assertTrue(executionTime > 0);
        assertTrue(cleanCompile);
        assertTrue(validPatch);
        assertNotNull(junitResult);
        assertEquals(0, failureCount);

    }
}