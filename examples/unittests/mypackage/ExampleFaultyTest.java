package mypackage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExampleFaultyTest {

    @Test
    public void testReturnTen() {
        ExampleFaulty example = new ExampleFaulty();
        int result = example.returnTen();
        assertEquals(10, result);
    }

    @Test
    public void emptyTest() {
        assertTrue(true);
    }

    @Test
    public void testReturnOneHundred() {
        ExampleFaulty example = new ExampleFaulty();
        int expected = 100;
        int actual = example.returnOneHundred();
        assertEquals(expected, actual);
    }

}
