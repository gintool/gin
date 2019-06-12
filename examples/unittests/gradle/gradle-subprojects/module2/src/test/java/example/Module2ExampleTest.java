package example;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class Module2ExampleTest {

    @Test
    public void testDoSomething() {
        Module2Example example = new Module2Example();
        int actual = example.doSomething();
        assertEquals(actual, 91);
    }

}