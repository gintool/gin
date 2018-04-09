package gin.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Result;

import static org.junit.Assert.*;

public class TestResultTest {

    TestResult testResult;
    Result result = new Result();

    private static final String patchedProgram = "public class SimpleExample() { }";
    private static final String expectedToString = "Patch Valid: true; Compiled: true; Time: 253.000000; Passed: true";

    @Before
    public void setUp() throws Exception {
        testResult = new TestResult(result, 253, true,  true, patchedProgram);
    }

    @Test
    public void getPatchedProgram() {
        assertEquals(patchedProgram, testResult.getPatchedProgram());
    }

    @Test
    public void getJunitResult() {
        assertEquals(result, testResult.getJunitResult());
    }

    @Test
    public void getExecutionTime() {
        assertEquals(253, testResult.getExecutionTime(), 1e-15);
    }

    @Test
    public void getCleanCompile() {
        assertTrue(testResult.getCleanCompile());
    }

    @Test
    public void getValidPatch() {
        assertTrue(testResult.getValidPatch());
    }

    @Test
    public void testToString() {
        assertEquals(expectedToString, testResult.toString());
    }
}