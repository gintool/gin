package mypackage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExampleTest {

    @Test
    public void testReturnTen() {
        Example example = new Example();
        int result = example.returnTen();
        assertEquals(10, result);
    }

    @Test
    public void emptyTest() {
        assertTrue(true);
    }

    @Test
    public void testReturnOneHundred() {
        Example example = new Example();
        int expected = 100;
        int actual = example.returnOneHundred();
        assertEquals(expected, actual);
    }

}
