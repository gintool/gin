package mypackage;

import static org.junit.Assert.*;

public class ExampleWithInnerClassTest {

    @org.junit.Test
    public void testSimpleMethod() {
        ExampleWithInnerClass example = new ExampleWithInnerClass();
        int result = example.simpleMethod();
        assertEquals(20, result);
    }

}
