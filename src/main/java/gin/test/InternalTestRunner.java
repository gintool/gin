package gin.test;

import gin.test.classloader.CacheClassLoader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.mdkt.compiler.CompiledCode;
import org.pmw.tinylog.Logger;

import gin.Patch;
import gin.test.classloader.ClassLoaderFactory;
import java.io.IOException;
import java.util.Set;

/**
 * Runs tests internally, through {@link CacheClassLoader} (default
 * ClassLoadder). The {@code ClassLoader} is created using the provided
 * {@link ClassLoaderFactory}.
 */
public class InternalTestRunner extends TestRunner {

    public static int count = 0;
    public static final String ISOLATED_TEST_RUNNER_METHOD_NAME = "runTests";

    protected ClassLoaderFactory classLoaderFactory;
    private CacheClassLoader classLoader;
    private boolean failFast;

    /**
     * Creates an InternalTestRunner given a {@code package.ClassName}, a
     * classpath string separated by colons if needed, a list of unit tests that
     * will be used to test patches, and a specific {@link ClassLoaderFactory}
     * to create {@code ClassLoaders} during the test cases execution.
     *
     * @param fullyQualifiedClassName class name including full package name
     * @param classPath               standard Java classpath format
     * @param unitTests               list of unit tests to be run against each
     *                                patch.
     * @param classLoaderFactory      a specific ClassLoaderFactory to load
     *                                modified classes during test runs
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests, ClassLoaderFactory classLoaderFactory) {
        super(fullyQualifiedClassName, classPath, unitTests);
        this.classLoaderFactory = classLoaderFactory;
    }

    /**
     * Creates an InternalTestRunner given a {@code package.ClassName}, a
     * classpath string separated by colons if needed, a test class that will be
     * used to test patches, and a specific {@link ClassLoaderFactory} to create
     * {@code ClassLoaders} during the test cases execution.
     *
     * @param fullyQualifiedClassName class name including full package name
     * @param classPath               standard Java classpath format
     * @param testClassName           name of the test class. The unit tests
     *                                will be retrieved via reflection
     * @param classLoaderFactory      a specific ClassLoaderFactory to load
     *                                modified classes during test runs
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, String testClassName, ClassLoaderFactory classLoaderFactory) {
        this(fullyQualifiedClassName, classPath, new LinkedList<UnitTest>(), classLoaderFactory);
        this.setTests(testsForClass(testClassName));
    }

    /**
     * Creates an InternalTestRunner given a {@code package.ClassName}, a
     * classpath string separated by colons if needed, and a list of unit tests
     * that will be used to test patches.
     *
     * @param fullyQualifiedClassName class name including full package name
     * @param classPath               standard Java classpath format
     * @param unitTests               list of unit tests to be run against each
     *                                patch
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests) {
        this(fullyQualifiedClassName, classPath, unitTests, ClassLoaderFactory.createDefaultGinClassLoader());
    }

    /**
     * Creates an InternalTestRunner given a {@code package.ClassName}, a
     * classpath string separated by colons if needed, and a test class that
     * will be used to test patches.
     *
     * @param fullyQualifiedClassName class name including full package name
     * @param classPath               standard Java classpath format
     * @param testClassName           name of the test class. The unit tests
     *                                will be retrieved via reflection
     */
    public InternalTestRunner(String fullyQualifiedClassName, String classPath, String testClassName) {
        this(fullyQualifiedClassName, classPath, new LinkedList<UnitTest>());
        this.setTests(testsForClass(testClassName));
    }

    public ClassLoaderFactory getClassLoaderFactory() {
        return classLoaderFactory;
    }

    public void setClassLoaderFactory(ClassLoaderFactory classLoaderFactory) {
        this.classLoaderFactory = classLoaderFactory;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Applies and compile the given patch, then run all unit tests against it.
     *
     * @param patch patch to apply
     * @param reps  number of times to run each test
     * @return the result of the test set run. See {@link UnitTest}
     */
    @Override
    public UnitTestResultSet runTests(Patch patch, int reps) {
        try {
            Logger.info("Preparing to run patch #" + (++count) + ": " + patch.toString());
            // Create a new class loader for every compilation, otherwise java will
            // cache the modified class for us
            this.classLoader = classLoaderFactory.createClassLoader(this.getClassPath());

            // Apply the patch.
            String patchedSource = patch.apply();
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
            List<UnitTestResult> results;
            if(patchValid) {
                // Compile
                CompiledCode code = Compiler.compile(this.getClassName(), patchedSource, this.getClassPath());
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
            UnitTestResultSet unitTestResultSet = new UnitTestResultSet(patch, patchValid, editsValid, compiledOK, noOp, results);
            Logger.info("\t|---> Results of " + unitTestResultSet.getResults().size() + " tests successful? = " + unitTestResultSet.allTestsSuccessful());
            Logger.info("\t|---> Execution time: " + unitTestResultSet.totalExecutionTime());
            return unitTestResultSet;
        } finally {
            try {
                this.classLoader.close();
            } catch (IOException ex) {
                Logger.error(ex, "Could not close CacheClassLoader.");
            }
        }
    }

    /**
     * Runs each of the tests against the modified class held in the class load,
     * rep times.
     *
     * @param reps        number of times to run each test
     * @param classLoader CacheClassLoader containing correct classpath and any
     *                    modified classes
     * @return the result of the test set run. See {@link UnitTest}
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
                if (failFast && !testResult.getPassed()) {
                    return results;
                }
            }
        }
        return results;
    }

    /**
     * Runs the test class for a modified class. Loads {@link JUnitBridge} using
     * a separate {@code ClassLoader} and invokes {@code JUnit} using
     * reflection. This allows us to have {@code JUnit} load all classes from a
     * isolated ClassLoader, enabling us to override the modified class with the
     * freshly compiled version.
     *
     * @return the result of the single test run. See {@link UnitTest}
     */
    private UnitTestResult runSingleTest(UnitTest test, CacheClassLoader classLoader, int rep) {

        Class<?> runnerClass = null;
        try {
            runnerClass = classLoader.loadClass(JUnitBridge.class.getName());
        } catch (ClassNotFoundException e) {
            Logger.error(e, "Could not load isolated test runner - class not found.");
            System.exit(-1);
        }

        Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            Logger.error(e, "Could not instantiate isolated test runner.");
            System.exit(-1);
        }

        Method method = null;
        try {
            method = runner.getClass().getMethod(JUnitBridge.BRIDGE_METHOD_NAME, UnitTest.class, int.class);
        } catch (NoSuchMethodException e) {
            Logger.error(e, "Could not run isolated tests runner, can't find method: " + ISOLATED_TEST_RUNNER_METHOD_NAME);
            System.exit(-1);
        }

        Set<Thread> threadsBefore = Thread.getAllStackTraces().keySet();
        try {
            UnitTestResult res = (UnitTestResult) method.invoke(runner, test, rep);
            return res;
        } catch (IllegalAccessException | InvocationTargetException e) {
            Logger.trace(e);
            UnitTestResult tempResult = new UnitTestResult(test, rep);
            tempResult.setExceptionType(e.getClass().getName());
            tempResult.setExceptionMessage(e.getMessage());
            tempResult.setPassed(false);
            return tempResult;
        } finally {
            cleanupHangingThreads(threadsBefore);
        }
    }

    private void cleanupHangingThreads(Set<Thread> threadsBefore) {
        Set<Thread> threadsAfter = Thread.getAllStackTraces().keySet();
        if (!threadsBefore.containsAll(threadsAfter)) {
            Logger.warn("Possible hanging threads remain after test: " + threadsBefore.size() + " -> " + threadsAfter.size());
            Logger.info("I'll try to kill them for you.");
            for (Thread thread : threadsAfter) {
                if (!threadsBefore.contains(thread)) {
                    thread.stop();
                }
            }
        }
    }

}
