package gin.test;

import gin.Patch;
import org.mdkt.compiler.CompiledCode;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Runs tests internally, through CacheClassLoader
 */
public class InternalTestRunner extends TestRunner {

    public static final String ISOLATED_TEST_RUNNER_METHOD_NAME = "runTests";
    @Serial
    private static final long serialVersionUID = -2348071089493178903L;
    /**
     * If set to true, the tests will stop at the first failure and the next
     * patch will be executed. You probably don't want to set this to true for
     * Automatic Program Repair.
     */
    private boolean failFast;

    /**
     * Create an InternalTestRunner given a package.ClassName, a classpath string separated by colons if needed,
     * and a list of unit tests that will be used to test patches.
     *
     * @param fullyQualifiedClassName Class name including full package name.
     * @param classPath               Standard Java classpath format.
     * @param unitTests               List of unit tests to be run against each patch.
     * @param failFast                Whether the test execution should stop at the first
     *                                failed test.
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests, boolean failFast) {
        super(fullyQualifiedClassName, classPath, unitTests);
        this.failFast = failFast;
    }

    /**
     * Create an InternalTestRunner given a package.ClassName, a classpath string separated by colons if needed,
     * and a list of unit tests that will be used to test patches.
     *
     * @param fullyQualifiedClassName Class name including full package name.
     * @param classPath               Standard Java classpath format.
     * @param testClassName           Fully qualified name of the test class to be run
     *                                against each patch.
     * @param failFast                Whether the test execution should stop at the first
     *                                failed test.
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, String testClassName, boolean failFast) {
        this(fullyQualifiedClassName, classPath, new LinkedList<>(), failFast);
        this.setTests(testsForClass(testClassName));
    }

    // Separated out so we can modify.
    private static int getNumberOfThreads() {
        return java.lang.Thread.activeCount();
    }

    /**
     * Returns whether this runner should fail fast. See {@link #failFast}.
     *
     * @return {@code true} if the runner should stop at the first failed test
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether this runner should fail fast. See {@link #failFast}.
     *
     * @param failFast {@code true} if the runner should stop at the first
     *                 failed test
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Apply and compile the given patch, then run all unit tests against it.
     *
     * @param patch Patch to apply.
     * @param reps  Number of times to run each test.
     * @return the results of the tests
     */
    public UnitTestResultSet runTests(Patch patch, Object metadata, int reps) {
        List<UnitTestResult> results;
        // Create a new class loader for every compilation, otherwise java will cache the modified class for us
        CacheClassLoader classLoader = new CacheClassLoader(this.getClassPath());
        try {
            // Apply the patch.
            String patchedSource = patch.apply(metadata);
            boolean patchValid = patch.lastApplyWasValid();
            List<Boolean> editsValid = patch.getEditsInvalidOnLastApply();
            // Did the code change as a result of applying the patch?
            boolean noOp = isPatchedSourceSame(patch.getSourceFile().toString(), patchedSource);
            //Initialise with default value
            boolean compiledOK = false;
            // Only tries to compile and run when the patch is valid
            // The patch might be invalid due to a couple of edits, which
            // drop to being no-ops; remaining edits might be ok so still
            // try compiling and then running in case of no-op
            Compiler compiler = new Compiler();
            if (patchValid) {
                // Compile
                CompiledCode code = compiler.compile(this.getClassName(), patchedSource, this.getClassPath());
                compiledOK = (code != null);
                // Run tests
                if (compiledOK) {
                    classLoader.setCustomCompiledCode(this.getClassName(), code.getByteCode());
                    results = runTests(reps, classLoader);
                } else {
                    results = emptyResults(reps);
                }
            } else {
                results = emptyResults(reps);
            }

            return new UnitTestResultSet(patch, patchedSource, patchValid, editsValid, compiledOK, compiler.getLastError(), noOp, results);
        } finally {
            try {
                classLoader.close();
            } catch (IOException ex) {
                Logger.error(ex, "Could not close CacheClassLoader.");
            }
        }
    }

    /**
     * Run each of the tests against the modified class held in the class load, rep times.
     *
     * @param reps        Number of times to run each test
     * @param classLoader CacheClassLoader containing correct classpath and any modified classes.
     */
    private List<UnitTestResult> runTests(int reps, CacheClassLoader classLoader) {

        List<UnitTest> testsToRun = this.getTests();
        List<UnitTestResult> results = new LinkedList<>();
        for (int r = 1; r <= reps; r++) {
            for (UnitTest testToRun : testsToRun) {
                // Run the test.
                UnitTestResult testResult = runSingleTest(testToRun, classLoader, r);
                // Save results.
                results.add(testResult);
                // If it is fail fast and the test failed, then return and stop
                // the execution.
                if (failFast && !testResult.getPassed()) {
                    return results;
                }
            }
        }

        return results;

    }

    /**
     * Run the test class for a modified class.
     * Loads JUnitBridge using a separate classloader and invokes jUnit using reflection.
     * This allows us to have jUnit load all classes from a CacheClassLoader, enabling us to override the modified
     * class with the freshly compiled version.
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
            runner = runnerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException e) {
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

        Object result;
        try {
            result = method.invoke(runner, test, rep);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
            UnitTestResult tempResult = new UnitTestResult(test, rep);
            tempResult.setExceptionType(e.getClass().getName());
            tempResult.setExceptionMessage(e.getMessage());
            tempResult.setPassed(false);
            result = tempResult;
        }

        int threadsAfter = getNumberOfThreads();

        if (threadsAfter != threadsBefore) {
            Logger.warn("Possible hanging threads remain after test");
        }

        return (UnitTestResult) result;

    }


}
