package gin;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.mdkt.compiler.InMemoryJavaCompiler;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

public class TestRunner {

    private static final int DEFAULT_REPS = 1;

    private File packageDirectory;
    private String className;

    public TestRunner(File packageDirectory, String className) {
        this.packageDirectory = packageDirectory;
        this.className = className;
    }

    public TestResult test(Patch patch) {
        return test(patch, DEFAULT_REPS);
    }

    public TestResult test(Patch patch, int reps) {

        // Apply the patch
        String patchedSource = patch.apply();

        // If unable to apply patch, report as invalid
        if (patchedSource == null) {
            return new TestResult(null, -1, false, false, "");
        }

        // Compile the patched sourceFile and test classes
        Class modifiedClass = compile(this.className, patchedSource);

        // If failed to compile, return with partial result
        if (modifiedClass == null) {
            return new TestResult(null, -1, false, true, patchedSource);
        }

        // Otherwise, run tests and return
        TestResult result = runTests(modifiedClass, reps);
        result.patchedProgram = patchedSource;

        return result;

    }


    private TestResult runTests(Class modifiedClass, int reps) {

        classLoader = new CacheClassLoader(workingDirectory);

        if (modifiedClass != null) {
            classLoader.store(this.className, modifiedClass);
        }

        Class<?> runnerClass = null;

        try {
            runnerClass = classLoader.loadClass(IsolatedTestRunner.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Invoke via reflection (List.class is OK because it just uses the string form of it)
        Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Method method = null;
        try {
            method = runner.getClass().getMethod("runTestClasses", List.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        List<String> testClasses = new LinkedList<>();
        testClasses.add(this.testName);

        try {
            method.invoke(runner, testClasses);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    /**
     * Compile the temporary (patched) source file and a copy of the test class.
     *
     * @return Boolean indicating whether the compilation was successful.
     */
    protected Class compile(String className, String source)  {

        Class<?> after = null;

        try {
            after = InMemoryJavaCompiler.newInstance().compile(className, source);
        } catch (Exception e) {
            System.err.println("Error compiling in memory: " + e);
            System.exit(-1);
        }

        return after;

    }

    /**
     * Run the tests against the patched class.
     *
     * @return TestResult giving the outcome of running jUnit.
     */
    private TestResult loadClassAndRunTests(int reps) {

        // Create a class loader initialised for the temp directory.
        URLClassLoader classLoader = null;

        try {
            classLoader = new URLClassLoader(new URL[]{new File(TMP_DIR).toURI().toURL()});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Load the Test class. The required class under test will be loaded from the same directory by jUnit.
        Class<?> loadedTestClass = null;
        try {
            String classname = FilenameUtils.removeExtension(new File(sourceFile.getFilename()).getName());
            loadedTestClass = classLoader.loadClass(classname + "Test");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        // Run the tests REPS times and calculate the mean via a running average
        double[] elapsed = new double[reps];
        Result result = null;
        for (int rep=0; rep < reps; rep++) {
            try {
                long start = System.nanoTime();
                result = jUnitCore.run(loadedTestClass);
                elapsed[rep] = System.nanoTime() - start;
            } catch (Exception e) {
                System.err.println("Error running junit: " + e);
                System.exit(-1);
            }
        }

        double thirdQuartile = new DescriptiveStatistics(elapsed).getPercentile(75);
        return new TestResult(result, thirdQuartile, true, true, "");

    }

    /**
     * Class to hold the junitResult of running jUnit.
     */
    public class TestResult {
        String patchedProgram = "";
        Result junitResult = null;
        double executionTime = -1;
        boolean compiled = false;
        boolean patchSuccess = false;
        public TestResult(Result result, double executionTime, boolean compiled, boolean patchedOK,
                          String patchedProgram) {
            this.junitResult = result;
            this.executionTime = executionTime;
            this.compiled = compiled;
            this.patchSuccess = patchedOK;
            this.patchedProgram = patchedProgram;
        }
        public String toString() {
            boolean junitOK = false;
            if (this.junitResult != null) {
                junitOK = this.junitResult.wasSuccessful();;
            }
            return String.format("Patch Valid: %b; Compiled: %b; Time: %f; Passed: %b", this.patchSuccess,
                    this.compiled, this.executionTime, junitOK);
        }
    }

}