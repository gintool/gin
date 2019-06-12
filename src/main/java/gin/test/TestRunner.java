package gin.test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.pmw.tinylog.Logger;

import gin.Patch;

/**
 * A TestRunner is defined by a class name, a class path, and a set of tests to run.
 * Once instantiated for a set of tests, it can be repeatedly invoked to run tests against new patches.
 */
public abstract class TestRunner {

    private final String packageName;
    private final String className;
    private final String classPath;
    private List<UnitTest> tests;

    public abstract UnitTestResultSet runTests(Patch patch, int reps) throws IOException, InterruptedException;

    // Constructor with a list of tests to run
    public TestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests) {
        this.className = fullyQualifiedClassName;
        this.classPath = classPath;
        this.tests = unitTests;
        if (className.contains(".")) {
            this.packageName = StringUtils.substringBeforeLast(className, ".");
        } else {
            this.packageName = "";
        }
    }

    public List<UnitTest> getTests() {
        return this.tests;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassNameWithoutPackage() {
        if (!packageName.isEmpty()) {
            return StringUtils.substringAfterLast(className, ".");
        }
        return className;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setTests(List<UnitTest> tests) {
        this.tests = tests;
    }

    public List<UnitTest> testsForClass(String testClassName) {

        CacheClassLoader classLoader = new CacheClassLoader(this.getClassPath());

        // Set up list of tests based on the class name
        List<UnitTest> tests = new LinkedList<>();

        Class clazz = null;

        try {
            clazz = classLoader.loadClass(testClassName);
        } catch (ClassNotFoundException e) {
            Logger.error("Failed to find test class: ");
        }

        List<FrameworkMethod> methods = new TestClass(clazz).getAnnotatedMethods(Test.class);

        for (FrameworkMethod eachTestMethod : methods){

            String methodName = eachTestMethod.getName();
            UnitTest test = new UnitTest(testClassName, methodName);
            tests.add(test);

        }

        return tests;

    }

    public LinkedList<UnitTestResult> emptyResults(int reps) {
        LinkedList<UnitTestResult> results = new LinkedList<>();
        for (int rep=1; rep<=reps; rep++) {
            for (UnitTest test : this.getTests()) {
                UnitTestResult result = new UnitTestResult(test, rep);
                results.add(result);
            }
        }
        return results;
    }

}
