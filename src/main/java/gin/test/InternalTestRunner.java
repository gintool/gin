package gin.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.mdkt.compiler.CompiledCode;
import org.pmw.tinylog.Logger;

import gin.Patch;

/**
 * Runs tests internally, through CacheClassLoader
 */
public class InternalTestRunner extends TestRunner {

    public static final String ISOLATED_TEST_RUNNER_METHOD_NAME = "runTests";

    private CacheClassLoader classLoader;

    /**
     * Create an InternalTestRunner given a package.ClassName, a classpath string separated by colons if needed,
     * and a list of unit tests that will be used to test patches.
     * @param fullyQualifiedClassName Class name including full package name.
     * @param classPath Standard Java classpath format.
     * @param unitTests List of unit tests to be run against each patch.
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests) {
        super(fullyQualifiedClassName, classPath, unitTests);
    }

    public InternalTestRunner(String fullyQualifiedClassName, String classPath, String testClassName) {
        this(fullyQualifiedClassName, classPath, new LinkedList<UnitTest>());
        this.setTests(testsForClass(testClassName));
    }


    /**
     * Apply and compile the given patch, then run all unit tests against it.
     * @param patch Patch to apply.
     * @param reps Number of times to run each test.
     * @return the results of the tests
     */
    public UnitTestResultSet runTests(Patch patch, int reps) {

        // Create a new class loader for every compilation, otherwise java will cache the modified class for us
        classLoader = new CacheClassLoader(this.getClassPath());

        // Apply the patch.
        String patchedSource = patch.apply();
        boolean patchValid = (patchedSource != null);

        // Compile
        CompiledCode code = null;
        if (patchValid) {
             code = Compiler.compile(this.getClassName(), patchedSource, this.getClassPath());
        }
        boolean compiledOK = (code != null);

        // Add to class loader and run tests
        List<UnitTestResult> results = null;
        if (compiledOK) {
            classLoader.setCustomCompiledCode(this.getClassName(), code);
            results = runTests(reps, classLoader);
        }

        if (!patchValid || !compiledOK) {
            results = emptyResults(reps);
        }

        return new UnitTestResultSet(patch, patchValid, compiledOK, results);

    }

    /**
     * Run each of the tests against the modified class held in the class load, rep times.
     * @param reps Number of times to run each test
     * @param classLoader CacheClassLoader containing correct classpath and any modified classes.
     * @return
     */
    private LinkedList<UnitTestResult> runTests(int reps, CacheClassLoader classLoader) {

        LinkedList<UnitTestResult> results = new LinkedList<>();

        for (int r=1; r <= reps; r++) {
            for (UnitTest test: this.getTests()) {
                results.add(runSingleTest(test, classLoader, r));
            }
        }

        return results;

    }

    /**
     * Run the test class for a modified class.
     * Loads JUnitBridge using a separate classloader and invokes jUnit using reflection.
     * This allows us to have jUnit load all classes from a CacheClassLoader, enabling us to override the modified
     * class with the freshly compiled version.
     * @return
     */
    private UnitTestResult runSingleTest(UnitTest test, CacheClassLoader classLoader, int rep) {

        Class<?> runnerClass = null;
        try {
            runnerClass = classLoader.loadClass(JUnitBridge.class.getName());
        } catch (ClassNotFoundException e) {
            Logger.error("Could not load isolated test runner - class not found.");
            System.exit(-1);
        }

        Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException e) {
            Logger.error("Could not instantiate isolated test runner: " + e);
            System.exit(-1);
        } catch (IllegalAccessException e) {
            Logger.error("Could not instantiate isolated test runner: " + e);
            System.exit(-1);
        }

        Method method = null;
        try {
            method = runner.getClass().getMethod(JUnitBridge.BRIDGE_METHOD_NAME, UnitTest.class, int.class);
        } catch (NoSuchMethodException e) {
            Logger.error("Could not run isolated tests runner, can't find method: " + ISOLATED_TEST_RUNNER_METHOD_NAME);
            System.exit(-1);
        }

        int threadsBefore = getNumberOfThreads();

        Object result = null;
        try {
            result = method.invoke(runner, test, rep);
        } catch (IllegalAccessException e) {
            Logger.trace(e);
        } catch (InvocationTargetException e) {
            Logger.trace(e);
        }

        int threadsAfter = getNumberOfThreads();

        if (threadsAfter != threadsBefore) {
            Logger.warn("Possible hanging threads remain after test");
        }
        
        UnitTestResult res = (UnitTestResult) result;

        return res;

    }

    // Separated out so we can modify.
    private static int getNumberOfThreads() {
        return java.lang.Thread.activeCount();
    }


}
