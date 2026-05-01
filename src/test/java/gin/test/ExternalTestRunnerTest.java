package gin.test;

import gin.Patch;
import gin.SourceFile;
import gin.SourceFileLine;
import gin.TestConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ExternalTestRunnerTest {

    static String packageName = "mypackage";
    ExternalTestRunner runnerReuse;
    ExternalTestRunner runnerMakeNew;
    String className = "Simple";
    String fullClassName = packageName + "." + className;
    String methodName = "returnsTrue()";
    String classPath = TestConfiguration.EXAMPLE_DIR_NAME;
    String testClassname = "myPackage.SimpleTest";
    String testMethodName = "testReturnsTrue";

    File packageDirectory = new File(TestConfiguration.EXAMPLE_DIR, packageName);
    File sourceFile = new File(packageDirectory, className + ".java");

    @BeforeClass
    public static void setUpClass() {
        String[] sourceFilenames = new String[]{
                "Poison.java",
                "ExampleFaultyTest.java",
                "ExampleFaulty.java"};

        for (String sourceFilename : sourceFilenames) {
            File packageDir = new File(TestConfiguration.EXAMPLE_DIR, packageName);
            File sourceFile = new File(packageDir, sourceFilename);
            new Compiler().compileFile(sourceFile, TestConfiguration.EXAMPLE_DIR_NAME);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
        resourcesDir = new File(resourcesDir, "mypackage");
        Files.deleteIfExists(new File(resourcesDir, "Poison.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ExampleFaultyTest.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ExampleFaulty.class").toPath());
    }

    @Before
    public void setUp() {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest(testClassname, testMethodName);
        tests.add(test);
        runnerReuse = new ExternalTestRunner(fullClassName, classPath, tests, false, false, false);
        runnerMakeNew = new ExternalTestRunner(fullClassName, classPath, tests, false, true, false);
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

        Compiler compiler = new Compiler();
        boolean success = runnerReuse.compileClassToTempDir(patchedSource, compiler);

        assertTrue(success);
        assertTrue(expectedClassPath.toFile().exists());

    }

    @Test
    public void testRunTests() throws IOException, InterruptedException {

        LinkedList<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.ExampleFaultyTest", "emptyTest");
        tests.add(test);
        UnitTest test2 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnTen");
        tests.add(test2);
        UnitTest test3 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnOneHundred");
        tests.add(test3);

        runnerReuse = new ExternalTestRunner(fullClassName, classPath, tests, false, false, false);

        List<String> targetMethodNames = new LinkedList<>();
        targetMethodNames.add(methodName);
        SourceFileLine sourceFileLine = new SourceFileLine(sourceFile.getPath(), targetMethodNames);
        Patch patch = new Patch(sourceFileLine);

        UnitTestResultSet resultSet = runnerReuse.runTests(patch, null, 1);
        List<UnitTestResult> results = resultSet.getResults();
        assertEquals(3, results.size());
        UnitTestResult result = results.get(0);
        assertTrue(result.getPassed());
        result = results.get(1);
        assertFalse(result.getPassed());
        result = results.get(2);
        assertTrue(result.getPassed());

    }

    @Test
    public void testRunTestsFailFast() throws IOException, InterruptedException {

        LinkedList<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.ExampleFaultyTest", "emptyTest");
        tests.add(test);
        UnitTest test2 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnTen");
        tests.add(test2);
        UnitTest test3 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnOneHundred");
        tests.add(test3);

        runnerReuse = new ExternalTestRunner(fullClassName, classPath, tests, false, false, true);

        List<String> targetMethodNames = new LinkedList<>();
        targetMethodNames.add(methodName);
        SourceFileLine sourceFileLine = new SourceFileLine(sourceFile.getPath(), targetMethodNames);
        Patch patch = new Patch(sourceFileLine);

        UnitTestResultSet resultSet = runnerReuse.runTests(patch, null, 1);
        List<UnitTestResult> results = resultSet.getResults();
        assertEquals(2, results.size());
        UnitTestResult result = results.get(0);
        assertTrue(result.getPassed());
        result = results.get(1);
        assertFalse(result.getPassed());

    }

    @Test
    public void testRunTestsNewSubProcess() throws IOException, InterruptedException {

        LinkedList<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.ExampleFaultyTest", "emptyTest");
        tests.add(test);
        UnitTest test2 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnTen");
        tests.add(test2);
        UnitTest test3 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnOneHundred");
        tests.add(test3);

        runnerMakeNew = new ExternalTestRunner(fullClassName, classPath, tests, true, true, false);

        List<String> targetMethodNames = new LinkedList<>();
        targetMethodNames.add(methodName);
        SourceFileLine sourceFileLine = new SourceFileLine(sourceFile.getPath(), targetMethodNames);
        Patch patch = new Patch(sourceFileLine);

        UnitTestResultSet resultSet = runnerMakeNew.runTests(patch, null, 1);
        List<UnitTestResult> results = resultSet.getResults();
        assertEquals(3, results.size());
        UnitTestResult result = results.get(0);
        assertTrue(result.getPassed());
        result = results.get(1);
        assertFalse(result.getPassed());
        result = results.get(2);
        assertTrue(result.getPassed());

    }

    @Test
    public void testRunTestsNewSubProcessFailFast() throws IOException, InterruptedException {

        LinkedList<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.ExampleFaultyTest", "emptyTest");
        tests.add(test);
        UnitTest test2 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnTen");
        tests.add(test2);
        UnitTest test3 = new UnitTest("mypackage.ExampleFaultyTest", "testReturnOneHundred");
        tests.add(test3);

        runnerMakeNew = new ExternalTestRunner(fullClassName, classPath, tests, true, true, true);

        List<String> targetMethodNames = new LinkedList<>();
        targetMethodNames.add(methodName);
        SourceFileLine sourceFileLine = new SourceFileLine(sourceFile.getPath(), targetMethodNames);
        Patch patch = new Patch(sourceFileLine);

        UnitTestResultSet resultSet = runnerMakeNew.runTests(patch, null, 1);
        List<UnitTestResult> results = resultSet.getResults();
        assertEquals(2, results.size());
        UnitTestResult result = results.get(0);
        assertTrue(result.getPassed());
        result = results.get(1);
        assertFalse(result.getPassed());

    }

    @Test
    public void testPoisonShouldFail() throws IOException, InterruptedException {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.Poison", "testPoison");
        tests.add(test);
        ExternalTestRunner externalRunner = new ExternalTestRunner(fullClassName, classPath, tests, false, false, false);
        UnitTestResultSet results = externalRunner.runTests(new Patch(new SourceFileLine(sourceFile, methodName)), null, 2);
        assertTrue(results.getResults().get(0).getPassed());
        // The second repetition fails
        assertFalse(results.getResults().get(1).getPassed());
    }

    @Test
    public void testPoisonShouldPass() throws IOException, InterruptedException {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.Poison", "testPoison");
        tests.add(test);
        ExternalTestRunner externalRunner = new ExternalTestRunner(fullClassName, classPath, tests, true, false, false);
        UnitTestResultSet results = externalRunner.runTests(new Patch(new SourceFileLine(sourceFile, methodName)), null, 2);
        assertTrue(results.getResults().get(0).getPassed());
        assertTrue(results.getResults().get(1).getPassed());
    }

    @Test
    public void testPoisonShouldPassWithJ() throws IOException, InterruptedException {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest("mypackage.Poison", "testPoison");
        tests.add(test);
        ExternalTestRunner externalRunner = new ExternalTestRunner(fullClassName, classPath, tests, false, true, false);
        UnitTestResultSet results = externalRunner.runTests(new Patch(new SourceFileLine(sourceFile, methodName)), null, 2);
        assertTrue(results.getResults().get(0).getPassed());
        assertTrue(results.getResults().get(1).getPassed());
    }
}
