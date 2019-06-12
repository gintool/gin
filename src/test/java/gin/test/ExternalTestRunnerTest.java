package gin.test;

import gin.Patch;
import gin.SourceFile;
import gin.SourceFileLine;
import gin.TestConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ExternalTestRunnerTest {

    ExternalTestRunner runnerReuse;
    ExternalTestRunner runnerMakeNew;
    String packageName = "mypackage";
    String className = "Simple";
    String fullClassName = packageName + "." + className;
    String methodName = "returnsTrue()";
    String classPath = TestConfiguration.EXAMPLE_DIR_NAME;
    String testClassname = "myPackage.SimpleTest";
    String testMethodName = "testReturnsTrue";
    String sourceFilename;

    File packageDirectory = new File(TestConfiguration.EXAMPLE_DIR, packageName);
    File sourceFile = new File(packageDirectory, className + ".java");


    @Before
    public void setUp() {
        List<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest(testClassname, testMethodName);
        tests.add(test);
        runnerReuse = new ExternalTestRunner(fullClassName, classPath, tests, false);
        runnerMakeNew = new ExternalTestRunner(fullClassName, classPath, tests, true);
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
    public void runTests() {
    }
}