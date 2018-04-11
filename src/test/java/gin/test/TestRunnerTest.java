package gin.test;

import gin.SourceFile;
import gin.test.TestRunner;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;

import static org.junit.Assert.*;

public class TestRunnerTest {

    private final static String examplePackageName = "src/test/resources/";
    private final static File examplePackageDirectory = new File(examplePackageName);
    private final static String exampleClassName = "ExampleTriangleProgram";
    private final static String exampleTestClassName = "ExampleTriangleProgramTest";
    private final static String exampleSourceFilename = examplePackageName + exampleClassName + ".java";
    private final static String exampleTestFilename = examplePackageName + exampleTestClassName + ".java";

    TestRunner testRunner;

    @Before
    public void setUp() {
        testRunner = new TestRunner(new File(examplePackageName), exampleClassName);
    }

    @Test
    public void testRunner() {
        TestRunner test = new TestRunner(examplePackageDirectory, exampleClassName);
        assertEquals(examplePackageDirectory, test.packageDirectory);
        assertEquals(exampleClassName, test.className);
        assertEquals(exampleTestClassName, test.testName);
    }

    @Test
    public void testCompile() {
        Class compiledClass = testRunner.compile("SimpleExample", "public class SimpleExample {} ");
        assertNotNull(compiledClass);
        assertEquals("SimpleExample", compiledClass.getSimpleName());
    }

    @Test
    public void testRunTests() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        URL[] classLoaderUrls = new URL[]{new URL("file:////./src/test/resources/")};
        URLClassLoader classLoader = new URLClassLoader(classLoaderUrls);
        Class exampleClass = classLoader.loadClass(exampleClassName);

        TestResult result = testRunner.runTests(exampleClass);

        assertTrue(result.getExecutionTime() > 0);
        assertTrue(result.getCleanCompile());
        assertTrue(result.getValidPatch());
        assertNotNull(result.getJunitResult());
        assertEquals(0, result.getJunitResult().getFailureCount());
    }

    @Test
    public void testOptimisation() {

    }

}