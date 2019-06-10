package apackage;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class Module1ExampleTest {

    @Test
    public void testDoSomething() {
        Module1Example example = new Module1Example();
        int actual = example.doSomething();
        assertEquals(actual, 88);

    }

}