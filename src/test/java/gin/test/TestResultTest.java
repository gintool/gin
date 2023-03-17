package gin.test;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestResultTest {

    private static final String expectedToString = "UnitTestResult ExampleClass.exampleMethod []. " + "Rep number: 0; " +
            "Passed: true; Timed out: false; Exception Type: N/A; Exception Message: N/A; Assertion Expected: N/A" +
            "; Assertion Actual: N/A; Execution Time: 0; CPU Time: 0;";
    UnitTestResult testResult;

    @Before
    public void setUp() throws Exception {
        UnitTest test = new UnitTest("ExampleClass", "exampleMethod");
        testResult = new UnitTestResult(test, 0);
        testResult.setPassed(true);
    }

    @Test
    public void getExecutionTime() {
        testResult.setExecutionTime(253);
        assertEquals(253, testResult.getExecutionTime(), 1e-15);
    }


    @Test
    public void testToString() {
        String actual = testResult.toString();
        assertEquals(expectedToString, actual);
    }

}
