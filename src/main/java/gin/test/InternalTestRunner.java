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

/**
 * Runs tests internally, through {@link CacheClassLoader} (default
 * ClassLoadder). The {@code ClassLoader} is created using the provided
 * {@link ClassLoaderFactory}.
 */
public class InternalTestRunner extends TestRunner {

    public static final String ISOLATED_TEST_RUNNER_METHOD_NAME = "runTests";

    protected ClassLoaderFactory classLoaderFactory;
    private CacheClassLoader classLoader;

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

    /**
     * Applies and compile the given patch, then run all unit tests against it.
     *
     * @param patch patch to apply
     * @param reps  number of times to run each test
     * @return the result of the test set run. See {@link UnitTest}
     */
    @Override
    public UnitTestResultSet runTests(Patch patch, int reps) {

        // Create a new class loader for every compilation, otherwise java will
        // cache the modified class for us
        this.classLoader = classLoaderFactory.createClassLoader(this.getClassPath());

        // Apply the patch.
        String patchedSource = patch.apply();
        boolean patchValid = patch.lastApplyWasValid();
        List<Boolean> editsValid = patch.getEditsInvalidOnLastApply();

        // Did the code change as a result of applying the patch?
        boolean noOp = isPatchedSourceSame(patch.getSourceFile().toString(), patchedSource);

        // Compile
        //if (patchValid) { // // might be invalid due to a couple of edits, which drop to being no-ops; remaining edits might be ok so try compiling
        CompiledCode code = Compiler.compile(this.getClassName(), patchedSource, this.getClassPath());
        //}
        boolean compiledOK = (code != null);

        // Add to class loader and run tests
        List<UnitTestResult> results = null;
        if (compiledOK) {
            byte[] byteCodeArray = code.getByteCode();
            this.classLoader.setCustomCompiledCode(this.getClassName(), byteCodeArray);
            results = runTests(reps, this.classLoader);
        }

        if (!patchValid || !compiledOK) {
            results = emptyResults(reps);
        }

        return new UnitTestResultSet(patch, patchValid, editsValid, compiledOK, noOp, results);

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
            Logger.error("Could not load isolated test runner - class not found.");
            System.exit(-1);
        }

        Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
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

        UnitTestResult res = (UnitTestResult) result;

        return res;

    }

    // Separated out so we can modify.
    private static int getNumberOfThreads() {
        return java.lang.Thread.activeCount();
    }

}
