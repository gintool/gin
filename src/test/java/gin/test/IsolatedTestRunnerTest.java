package gin.test;

import org.junit.Before;
import org.junit.Test;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class IsolatedTestRunnerTest {

    IsolatedTestRunner runner;

    @Test
    public void runTestClasses() throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {

        ClassLoader classLoader = new CacheClassLoader(new File("/Users/david/Documents/gin/src/test/resources/"));
        classLoader.loadClass("ExampleTriangleProgramTest");

        Class runnerClass = classLoader.loadClass(IsolatedTestRunner.class.getName());

        Object runner = runnerClass.newInstance();

        String methodName = "runTestClasses";
        Method method = runner.getClass().getMethod("runTestClasses", List.class);

        List<String> testClasses = new LinkedList<>();
        testClasses.add("ExampleTriangleProgramTest");

        Object result = method.invoke(runner, testClasses);

    }
}