import gin.edit.DeleteStatement;
import gin.test.CacheClassLoader;
import gin.test.TestResult;
import gin.test.TestRunner;
import org.apache.commons.io.FileUtils;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

public class IntegrationTests {

    private final static String examplePackageName = "src/test/resources/";
    private final static File examplePackageDirectory = new File(examplePackageName);

    private final static File RESOURCE_DIR = new File("./src/test/resources");
    private final static String exampleClassName = "ATriangle";
    private final static String exampleSourceFilename = examplePackageName + exampleClassName + ".java";
    private final static File exampleSourceFile = new File(exampleSourceFilename);

    private final static String exampleQuickClassName = "QuickTriangle";
    private final static String exampleQuickSourceFilename = examplePackageName + exampleQuickClassName + ".java";
    private final static File exampleQuickSourceFile = new File(exampleQuickSourceFilename);

    public static void main(String args[]) throws Exception {
        testRunTests();
        testOptimisation();
    }

    /**
     * Test the ATriangle class through the testrunner, ensure it passes.
     * @throws Exception
     */
    public static void testRunTests() throws Exception {

        System.out.println("TestRunTests");

        TestRunner testRunner = new TestRunner(RESOURCE_DIR, exampleClassName);

        Charset charset = null;
        String triangleSrc = FileUtils.readFileToString(exampleSourceFile, charset);

        Class triangleClass = InMemoryJavaCompiler.newInstance().compile("ATriangle", triangleSrc);

        TestResult result = testRunner.runTests(triangleClass, 1);

        assert(result.getExecutionTime() > 0);
        assert(result.getCleanCompile());
        assert(result.getValidPatch());
        assert(result.getJunitResult() != null);
        assert(0 == result.getJunitResult().getFailureCount());

    }

    /**
     * Test that an optimisation actually works. Delete a delay and check timing.
     */
    public static void testOptimisation() {

        System.out.println("TestOptimisation");

        SourceFile sourceFile = new SourceFile(exampleSourceFilename);

        TestRunner testRunner = new TestRunner(examplePackageDirectory, exampleClassName);

        Patch emptyPatch = new Patch(sourceFile);
        TestResult result = testRunner.test(emptyPatch, 10);
        double originalTime = result.getExecutionTime();

        Patch deleteDelayPatch = new Patch(sourceFile);
        DeleteStatement edit = new DeleteStatement(1);
        deleteDelayPatch.add(edit);

        TestRunner testRunner2 = new TestRunner(new File(examplePackageName), exampleClassName);

        TestResult newResult = testRunner2.test(deleteDelayPatch, 10);
        double newTime = newResult.getExecutionTime();

        System.out.println("Original time: " + originalTime);
        System.out.println("New time: " + newTime);

        assert(originalTime > newTime);


    }



}
