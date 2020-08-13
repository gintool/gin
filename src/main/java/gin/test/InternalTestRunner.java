package gin.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.mdkt.compiler.CompiledCode;
import org.pmw.tinylog.Logger;

import gin.Patch;
import java.io.IOException;
import java.util.Set;

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
        try {
            // Apply the patch.
            String patchedSource = patch.apply();
            boolean patchValid = patch.lastApplyWasValid();
            List<Boolean> editsValid = patch.getEditsInvalidOnLastApply();

            // Did the code change as a result of applying the patch?
            boolean noOp = isPatchedSourceSame(patch.getSourceFile().toString(), patchedSource);

            // Compile
            CompiledCode code = null;
            //if (patchValid) { // // might be invalid due to a couple of edits, which drop to being no-ops; remaining edits might be ok so try compiling
                 code = Compiler.compile(this.getClassName(), patchedSource, this.getClassPath());
            //}
            boolean compiledOK = (code != null);

            // Add to class loader and run tests
            List<UnitTestResult> results = null;
            if (compiledOK) {
                classLoader.setCustomCompiledCode(this.getClassName(), code.getByteCode());
                results = runTests(reps, classLoader);
            }

            if (!patchValid || !compiledOK) {
                results = emptyResults(reps);
            }

            return new UnitTestResultSet(patch, patchValid, editsValid, compiledOK, noOp, results);
        } finally {
            try {
                if(this.classLoader != null) {
                    this.classLoader.close();
                }
            } catch (IOException ex) {
                Logger.error(ex, "Could not close CacheClassLoader.");
            }
        }
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
                if(!threadsBefore.contains(thread)) {
                    Logger.debug("Found the following hanging thread:");
                    Logger.debug("\t|---> Thread hanging: " + thread.getName() + " (ID: " + thread.getId() + ")");
                    Logger.debug("\t|---> Group: " + thread.getThreadGroup().getName());
                    Logger.debug("\t|---> State: " + thread.getState());
                    Logger.debug("\t|---> Is Daemon? " + thread.isDaemon());
                    Logger.debug("\t|---> Stacktrace:");
                    for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
                        Logger.debug("\t\t|---> " + stackTraceElement);
    }
                    thread.stop();
                    Logger.debug("Is it interrupted? " + thread.isInterrupted());
    }
            }
        }
    }

}
