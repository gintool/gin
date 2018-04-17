package gin.test;

import gin.Patch;
import gin.SourceFile;
import gin.edit.DeleteStatement;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class TestRunnerTest {

    private final static String examplePackageName = "src/test/resources/";
    private final static File examplePackageDirectory = new File(examplePackageName);
    private final static String exampleClassName = "ATriangle";
    private final static String exampleTestClassName = "ATriangleTest";
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





}