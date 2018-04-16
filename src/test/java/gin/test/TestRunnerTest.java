package gin.test;

import gin.Patch;
import gin.SourceFile;
import gin.edit.DeleteStatement;
import org.junit.Before;
import org.junit.Test;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.net.*;

import static org.junit.Assert.*;

public class TestRunnerTest {

    private final static String examplePackageName = "src/test/resources/";
    private final static File examplePackageDirectory = new File(examplePackageName);
    private final static String exampleClassName = "Triangle";
    private final static String exampleTestClassName = "TriangleTest";
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
    public void testRunTests() throws MalformedURLException, ClassNotFoundException {

        URL[] classLoaderUrls = new URL[]{new URL("file:////./src/test/resources/classes/")};
        URLClassLoader classLoader = new URLClassLoader(classLoaderUrls);
        Class exampleClass = classLoader.loadClass(exampleClassName);

        TestResult result = testRunner.runTests(exampleClass, 1);

        assertTrue(result.getExecutionTime() > 0);
        assertTrue(result.getCleanCompile());
        assertTrue(result.getValidPatch());
        assertNotNull(result.getJunitResult());
        assertEquals(0, result.getJunitResult().getFailureCount());

    }

    /*
        This test does not work in IntelliJ.
        The class reload fails. Probably something to do with the way IntelliJ calls jUnit.
     */
    @Test
    public void testOptimisation() {

        SourceFile sourceFile = new SourceFile(exampleSourceFilename);

        Patch emptyPatch = new Patch(sourceFile);
        TestResult result = testRunner.test(emptyPatch, 10);
        double originalTime = result.getExecutionTime();

        Patch deleteDelayPatch = new Patch(sourceFile);
        DeleteStatement edit = new DeleteStatement(1);
        deleteDelayPatch.add(edit);

        TestRunner testRunner2 = new TestRunner(new File(examplePackageName), exampleClassName);

        TestResult newResult = testRunner2.test(deleteDelayPatch, 10);
        double newTime = newResult.getExecutionTime();

        System.out.println(newResult.junitResult.wasSuccessful());

        assertTrue(originalTime > newTime);


    }



}