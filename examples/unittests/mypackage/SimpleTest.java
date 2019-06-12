package mypackage;

import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleTest {

    @Test
    public void testReturnsTrue() {
        Simple s = new Simple();
        boolean result = s.returnsTrue();
        assertTrue(result);
    }

    @Test
    public void otherTest() {
        Simple s = new Simple();
        boolean result = s.returnsTrue();
        assertTrue(result);
    }

}