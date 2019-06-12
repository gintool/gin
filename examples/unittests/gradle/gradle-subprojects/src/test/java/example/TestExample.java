package example;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TestExample {

    @Test
    public void testGetValue() {
        Example f = new Example();
        assertEquals(f.getValue(), 100);
    }

}

