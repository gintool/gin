import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class ErrorTest {

    Error error;
    List<String> nullPointer;

    @Before
    public void setUp() throws Exception {
        error = new Error();
        nullPointer = null;
    }

    @Test
    public void testException() throws Exception {
        error.returnTen(5);
    }

    // Timeout is in ms
    @Test(timeout=1500)
    public void testTimeout() throws Exception {
        error.thisMethodTakesASecond();
    }

    @Test
    public void testAssertionError() throws Exception {
        int shouldBeTen = error.returnTen(0);
        assertEquals(15, shouldBeTen);
    }

    // Test will be ignored, but considered passed
    @Ignore
    @Test
    public void testIgnoredTest() throws Exception {
        error.returnTen(5);
    }

    // Test will be ignored, but considered passed
    @Test
    public void testFalseAssumption() throws Exception {
        assumeTrue(false);
        int shouldBeTen = error.returnTen(0);
        assertEquals(15, shouldBeTen);
    }

    @Test
    public void testFaultyTest() throws Exception {
        nullPointer.get(0);
    }

}
