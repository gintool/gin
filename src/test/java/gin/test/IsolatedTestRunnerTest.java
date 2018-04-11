package gin.test;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class IsolatedTestRunnerTest {

    File RESOURCES_DIR = new File("./src/test/resources/");
    File SRC_FILE = new File(RESOURCES_DIR, "ExampleTriangleProgram.java");

    @Test
    public void runTestClasses() throws ClassNotFoundException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {

        // Expected source is just the source in SRC_FILE
        String expectedSrc = null;
        try {
            Charset charSet = null; // platform default
            expectedSrc = FileUtils.readFileToString(SRC_FILE, charSet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertNotNull(expectedSrc);

        ClassLoader classLoader = new CacheClassLoader(RESOURCES_DIR);

        Class runnerClass = classLoader.loadClass("gin.test.IsolatedTestRunner");
        Object runner = runnerClass.newInstance();
        Method method = runnerClass.getMethod("runTestClasses", List.class, int.class);
        List<String> testClasses = new LinkedList<>();
        testClasses.add("ExampleTriangleProgramTest");
        TestResult result = (TestResult)method.invoke(runner, testClasses, 1);

        assertTrue(result.getExecutionTime() > 0);
        assertTrue(result.getCleanCompile());
        assertTrue(result.getValidPatch());
        assertNotNull(result.getJunitResult());
        assertEquals(0, result.getJunitResult().getFailureCount());

    }
}