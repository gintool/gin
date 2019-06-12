package gin.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;

import java.util.LinkedList;
import java.util.List;


import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.InMemoryJavaCompiler;

import gin.Patch;
import gin.SourceFileLine;
import gin.TestConfiguration;
import gin.edit.line.DeleteLine;

public class TestRunnerTest {

    private CacheClassLoader cacheClassLoader;
    private InternalTestRunner internalTestRunner;

    private String packageName = "mypackage";
    private String className = "Simple";
    private String fullClassName = packageName + "." + className;
    private String methodName = "returnsTrue()";

    private String testClassName = "SimpleTest";
    private String testMethodName = "testReturnsTrue";
    private String fullTestClassName = packageName + "." + testClassName;
    private String otherTestMethodName = "otherTest";

    private File packageDir = new File(TestConfiguration.EXAMPLE_DIR, packageName);
    private File sourceFile = new File(packageDir, className + ".java");

    private List<UnitTest> testSet;
    private SourceFileLine sourceFileLine;


    @Before
    public void setUp() throws Exception {

        cacheClassLoader = new CacheClassLoader(TestConfiguration.EXAMPLE_DIR_NAME);

        UnitTest test = new UnitTest(fullTestClassName, testMethodName);
        testSet = new LinkedList<>();
        testSet.add(test);

        internalTestRunner = new InternalTestRunner(fullClassName, TestConfiguration.EXAMPLE_DIR_NAME, testSet);

        List<String> targetMethodNames = new LinkedList<>();
        targetMethodNames.add(methodName);

        sourceFileLine = new SourceFileLine(sourceFile.getPath(), targetMethodNames);

        buildExampleClasses();


    }

    // Compile source files
    private void buildExampleClasses()  {

        String[] sourceFilenames = new String[]{
                this.className + ".java",
                this.testClassName + ".java",
                "Example.java",
                "ExampleTest.java",
                "ExampleInterface.java",
                "ExampleBase.java",
                "ExampleWithInnerClass.java",
                "ExampleWithInnerClassTest.java"};

        for (String sourceFilename: sourceFilenames) {
            File packageDir = new File(TestConfiguration.EXAMPLE_DIR, packageName);
            File sourceFile = new File(packageDir, sourceFilename);
            Compiler.compileFile(sourceFile, TestConfiguration.EXAMPLE_DIR_NAME);
        }

    }

    @Test
    public void testRunnerConstructor() {
        assertEquals(fullClassName, internalTestRunner.getClassName());
        assertEquals(fullTestClassName + "." + testMethodName, internalTestRunner.getTests().get(0).getTestName());
    }

    @Test
    public void testEmptyPatch() {

        Patch patch = new Patch(sourceFileLine);

        UnitTestResultSet resultSet = internalTestRunner.runTests(patch, 1);

        List<UnitTestResult> results = resultSet.getResults();
        UnitTestResult result = results.get(0);

        assertTrue(result.getPassed());

    }

    @Test
    public void testPatchWorks() {

        Patch deletePatch = new Patch(sourceFileLine);

        DeleteLine edit = new DeleteLine(sourceFile.getAbsolutePath(), 7); // deletes result=10 hence introducing a bug
        deletePatch.add(edit);

        UnitTestResultSet modifiedResultSet = internalTestRunner.runTests(deletePatch, 1);

        assertTrue(modifiedResultSet.getValidPatch());
        assertTrue(modifiedResultSet.getCleanCompile());
        assertFalse(modifiedResultSet.allTestsSuccessful());

        UnitTestResult modifiedResult = modifiedResultSet.getResults().get(0);
        assertFalse(modifiedResult.getPassed());

    }


    @Test
    public void testMultipleTestsProvided() {

        LinkedList<UnitTest> tests = new LinkedList<>();
        UnitTest test = new UnitTest(fullTestClassName, testMethodName);
        tests.add(test);
        UnitTest test2 = new UnitTest(fullTestClassName, otherTestMethodName);
        tests.add(test2);

        InternalTestRunner internalTestRunner;
        internalTestRunner = new InternalTestRunner(fullClassName, TestConfiguration.EXAMPLE_DIR.getAbsolutePath(), tests);

        Patch patch = new Patch(sourceFileLine);

        UnitTestResultSet resultSet = internalTestRunner.runTests(patch, 1);
        List<UnitTestResult> results = resultSet.getResults();
        UnitTestResult result = results.get(0);
        assertTrue(result.getPassed());

    }

    @Test
    public void testUseOfSuperClass() {

        LinkedList<UnitTest> tests = new LinkedList<>();

        UnitTest test = new UnitTest("mypackage.ExampleTest", "testReturnOneHundred");
        tests.add(test);

        String classPath = TestConfiguration.EXAMPLE_DIR.getAbsolutePath();

        File exampleSourceFile = new File(new File(TestConfiguration.EXAMPLE_DIR, "mypackage"), "Example.java");

        InternalTestRunner internalTestRunner ;
        internalTestRunner = new InternalTestRunner("mypackage.Example", classPath, tests);

        LinkedList<String> methods = new LinkedList<>();
        methods.add("returnOneHundred()");

        SourceFileLine sourceFile = new SourceFileLine(exampleSourceFile, "returnOneHundred()");

        Patch patch = new Patch(sourceFile);

        UnitTestResultSet resultSet = internalTestRunner.runTests(patch, 1);
        List<UnitTestResult> results = resultSet.getResults();
        UnitTestResult result = results.get(0);

        assertTrue(result.getPassed());

    }

    @Test
    public void testInnerClass() {

        LinkedList<UnitTest> tests = new LinkedList<>();

        UnitTest test = new UnitTest("mypackage.ExampleWithInnerClassTest", "testSimpleMethod");
        tests.add(test);

        InternalTestRunner internalTestRunner;
        String classPath = TestConfiguration.EXAMPLE_DIR_NAME;
        String classWithInnerName = "mypackage.ExampleWithInnerClass";
        internalTestRunner = new InternalTestRunner(classWithInnerName, classPath, tests);

        LinkedList<String> methods = new LinkedList<>();
        methods.add("simpleMethod()");

        File sourceWithInner = new File(TestConfiguration.EXAMPLE_DIR,"mypackage/ExampleWithInnerClass.java");
        SourceFileLine sourceFile = new SourceFileLine(sourceWithInner, "simpleMethod()");

        Patch patch = new Patch(sourceFile);

        UnitTestResultSet resultSet = internalTestRunner.runTests(patch, 1);
        List<UnitTestResult> results = resultSet.getResults();
        UnitTestResult result = results.get(0);

        assertTrue(result.getPassed());

    }

    /**
     * Test that compiling a class that implements an interface works, where the compiled class for that interface
     * is on the classpath.
     * @throws Exception
     */
    @Test
    public void usesInterfaceAndInterfaceClassOnClassPath() {


        String srcCode = "package mypackage;\n" +
                "public class NewExample implements ExampleInterface {\n" +
                "\n" +
                "    public static void main(String args[]) {\n" +
                "        System.out.println(\"This class doesn't really do anything.\");\n" +
                "    }\n" +
                "\n" +
                "    public void exampleMethod() {\n" +
                "        System.out.println(\"Implementing example method.\");\n" +
                "    }\n" +
                "\n" +
                "}";

        CompiledCode code = Compiler.compile("mypackage.NewExample", srcCode, TestConfiguration.EXAMPLE_DIR_NAME);

        CacheClassLoader loader = new CacheClassLoader(TestConfiguration.EXAMPLE_DIR_NAME);
        loader.setCustomCompiledCode("mypackage.NewExample", code);

        try {
            Class example = loader.findClass("mypackage.NewExample");
        } catch (ClassNotFoundException e) {
            fail("Could not load NewExample class.");
        }

    }

    @After
    public void tearDown() throws Exception {
            File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
            resourcesDir = new File(resourcesDir, "mypackage");
            Files.deleteIfExists(new File(resourcesDir, "ExampleInterface.class").toPath());
            Files.deleteIfExists(new File(resourcesDir, "Example.class").toPath());
            Files.deleteIfExists(new File(resourcesDir, "ExampleBase.class").toPath());
            Files.deleteIfExists(new File(resourcesDir, "ExampleTest.class").toPath());
            Files.deleteIfExists(new File(resourcesDir, "ExampleWithInnerClass.class").toPath());
            Files.deleteIfExists(new File(resourcesDir, "ExampleWithInnerClassTest.class").toPath());
            Files.deleteIfExists(new File(resourcesDir, "ExampleWithInnerClass$MyInner.class").toPath());
    }
}
