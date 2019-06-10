package gin.test;

import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Executable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.pmw.tinylog.Logger;

// see https://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836
// timeout annotation based on: https://gist.github.com/henrrich/185503f10cbb2499a0dc75ec4c29c8f2 and https://www.baeldung.com/java-reflection-change-annotation-params

/** 
 * Runs a given test in the same JVM as this class.
 */
public class JUnitBridge {

    public static final String BRIDGE_METHOD_NAME = "runTest";

    /**
     * This method is called using reflection to ensure tests are run in an environment that employs a separate
     * classloader.
     * @param test the unit test to run
     * @param rep the number of times to repeat the test
     * @return the test results
     */
    public UnitTestResult runTest(UnitTest test, int rep) {

        UnitTestResult result = new UnitTestResult(test, rep);

        Request request = null;

        try {
            request = buildRequest(test);

        } catch (ClassNotFoundException e) {
            Logger.error("Unable to find test class file: " + test);
            Logger.error("Is the class file on provided classpath?");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        } catch (NoSuchMethodException e) {
            Logger.error(e.getMessage());
            Logger.error("Note that parametirised JUnit tetsts are not allowed in Gin.");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        } catch (NoSuchFieldException e) {
            Logger.error("Exception when instrumenting tests with a timeout: " + e);
            Logger.error(e.getMessage());
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        
        } catch (IllegalAccessException e) {
            Logger.error("Exception when instrumenting tests with a timeout: " + e);
            Logger.error(e.getMessage());
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        JUnitCore jUnitCore = new JUnitCore();

        jUnitCore.addListener(new TestRunListener(result));

        try {
            jUnitCore.run(request);
        } catch (Exception e) {
            Logger.error("Error running junit: " + e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        return result;

    }

    public Request buildRequest(UnitTest test) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

        Class<?> clazz = null;

        String testClassname = test.getFullClassName();
        ClassLoader loader = this.getClass().getClassLoader();

        clazz = loader.loadClass(testClassname);

        String methodName = test.getMethodName();

        annotateTestWithTimeout(clazz, methodName, test.getTimeoutMS());

        return Request.method(clazz, methodName);

    }

     // A hack to add a timeout to the method using Java reflection.
     // It also checks that a given test method exists. Parametirised test methods are not allowed (following JUnit standard).
    protected static void annotateTestWithTimeout(Class<?> clazz, String methodName, long timeout) throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

        Field annotations = Executable.class.getDeclaredField("declaredAnnotations");
        annotations.setAccessible(true);

        Class<?> clazzCopy = clazz;

        boolean methodFound = false;

        while ((!methodFound) && (clazzCopy != java.lang.Object.class)) {
       
            try {
                Method m = clazzCopy.getDeclaredMethod(methodName);
                methodFound = true;
                m.getAnnotation(Annotation.class);
                Map<Class<? extends Annotation>, Annotation> map;
                map = (Map<Class<? extends Annotation>, Annotation>) annotations.get(m);
                org.junit.Test jTest = (org.junit.Test) map.get(org.junit.Test.class);
                if (jTest != null) {
                    ModifiableTest newTest = new ModifiableTest(timeout, jTest);
                    map.put(org.junit.Test.class, newTest);
                }

            } catch (NoSuchMethodException e) {
            
                clazzCopy = clazzCopy.getSuperclass();
            
            }
        }

        if (!methodFound) {
            throw new NoSuchMethodException("Test method " + methodName + " not found in " + clazz.getName());
        }        

    }


}
