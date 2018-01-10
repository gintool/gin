package gin;

import org.junit.runner.Result;
import org.mdkt.compiler.InMemoryJavaCompiler;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class TestRunner {

    private static final int DEFAULT_REPS = 1;

    private File packageDirectory;
    private String className;
    private String testName;

    public TestRunner(File packageDirectory, String className) {
        this.packageDirectory = packageDirectory;
        this.className = className;
        this.testName = className + "Test";
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

    /**
     * Compile the temporary (patched) source file and a copy of the test class.
     *
     * @return Boolean indicating whether the compilation was successful.
     */
    protected Class compile(String className, String source)  {

        Class<?> compiledClass = null;

        try {
            compiledClass = InMemoryJavaCompiler.newInstance().compile(className, source);
        } catch (Exception e) {
            System.err.println("Error compiling class " + className + " in memory: " + e);
            System.err.println("Source was: " + source);
            System.exit(-1);
        }

        return compiledClass;

    }

    /**
     * Run the test class for a modified class.
     * Loads IsolatedTestRunner using a separate classloader and invokes jUnit using reflection.
     * This allows us to have jUnit load all classes from a CacheClassLoader, enabling us to override the modified
     * class with the freshly compiled version.
     * @param modifiedClass The compiled class.
     * @param reps The number of repetitions (primarily for timing measurements)
     * @return
     */
    private TestResult runTests(Class modifiedClass, int reps) {

        CacheClassLoader classLoader = new CacheClassLoader(this.packageDirectory);
        classLoader.store(this.className, modifiedClass);

        Class<?> runnerClass = null;

        try {
            runnerClass = classLoader.loadClass(IsolatedTestRunner.class.getName());
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load isolated test runner - class not found.");
            System.exit(-1);
        }

        Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException e) {
            System.err.println("Could not instantiate isolated test runner: " + e);
            System.exit(-1);
        } catch (IllegalAccessException e) {
            System.err.println("Could not instantiate isolated test runner: " + e);
            System.exit(-1);
        }

        Method method = null;
        String methodName = "runTestClasses";
        try {
            method = runner.getClass().getMethod("runTestClasses", List.class);
        } catch (NoSuchMethodException e) {
            System.err.println("Could not run isolated test runner, can't find method: " + methodName);
            System.exit(-1);
        }

        List<String> testClasses = new LinkedList<>();
        testClasses.add(this.testName);

        Object result = null;
        try {
            result = method.invoke(runner, testClasses);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return (TestResult)result;

    }



}