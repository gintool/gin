package gin.test;

import gin.Patch;
import gin.SourceFile;
import gin.SourceFileLine;
import gin.TestConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;

public class ExternalTestRunnerTest {

    ExternalTestRunner runnerReuse;
    ExternalTestRunner runnerMakeNew;
    static String packageName = "mypackage";
    String className = "Simple";
    String fullClassName = packageName + "." + className;
    String methodName = "returnsTrue()";
    String classPath = TestConfiguration.EXAMPLE_DIR_NAME;
    String testClassname = "myPackage.SimpleTest";
    String testMethodName = "testReturnsTrue";
    String sourceFilename;

    File packageDirectory = new File(TestConfiguration.EXAMPLE_DIR, packageName);
    File sourceFile = new File(packageDirectory, className + ".java");

    @BeforeClass
    public static void setUpClass() {
        String[] sourceFilenames = new String[]{
            "Poison.java"};

        for (String sourceFilename : sourceFilenames) {
            File packageDir = new File(TestConfiguration.EXAMPLE_DIR, packageName);
            File sourceFile = new File(packageDir, sourceFilename);
            Compiler.compileFile(sourceFile, TestConfiguration.EXAMPLE_DIR_NAME);
        }
    }

    @Before
    public void setUp() {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest(testClassname, testMethodName);
        tests.add(test);
        runnerReuse = new ExternalTestRunner(fullClassName, classPath, tests, false, false);
        runnerMakeNew = new ExternalTestRunner(fullClassName, classPath, tests, false, true);
    }

    @Test
    public void testCreateTempDirectory() throws IOException {

        runnerReuse.createTempDirectory();

        Path tmpDir = runnerReuse.getTemporaryDirectory();
        assertTrue(tmpDir.toFile().exists());

        Path packageDir = tmpDir.resolve("mypackage");
        assertTrue(packageDir.toFile().exists());

    }

    @Test
    public void testCompileClassToTempDir() throws IOException {

        runnerReuse.createTempDirectory();

        SourceFile sf = new SourceFileLine(sourceFile, methodName);
        Patch emptyPatch = new Patch(sf);
        String patchedSource = emptyPatch.apply();

        Path expectedClassPath = runnerReuse.getTemporaryPackageDirectory().resolve(className + ".class");

        boolean success = runnerReuse.compileClassToTempDir(patchedSource);

        assertTrue(success);
        assertTrue(expectedClassPath.toFile().exists());

    }

    @Test
    public void testPoisonShouldFail() throws IOException, InterruptedException {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.Poison", "testPoison");
        tests.add(test);
        ExternalTestRunner externalRunner = new ExternalTestRunner(fullClassName, classPath, tests, false, false);
        UnitTestResultSet results = externalRunner.runTests(new Patch(new SourceFileLine(sourceFile, methodName)), 2);
        assertTrue(results.getResults().get(0).getPassed());
        // The second repetition fails
        assertFalse(results.getResults().get(1).getPassed());
    }
    
    @Test
    public void testPoisonShouldPass() throws IOException, InterruptedException {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.Poison", "testPoison");
        tests.add(test);
        ExternalTestRunner externalRunner = new ExternalTestRunner(fullClassName, classPath, tests, true, false);
        UnitTestResultSet results = externalRunner.runTests(new Patch(new SourceFileLine(sourceFile, methodName)), 2);
        assertTrue(results.getResults().get(0).getPassed());
        assertTrue(results.getResults().get(1).getPassed());
    }
    
    @Test
    public void testPoisonShouldPassWithJ() throws IOException, InterruptedException {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.Poison", "testPoison");
        tests.add(test);
        ExternalTestRunner externalRunner = new ExternalTestRunner(fullClassName, classPath, tests, false, true);
        UnitTestResultSet results = externalRunner.runTests(new Patch(new SourceFileLine(sourceFile, methodName)), 2);
        assertTrue(results.getResults().get(0).getPassed());
        assertTrue(results.getResults().get(1).getPassed());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
        resourcesDir = new File(resourcesDir, "mypackage");
        Files.deleteIfExists(new File(resourcesDir, "Poison.class").toPath());
    }
}
