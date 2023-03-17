package gin.util;

import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Giovani
 */
public class HotMethodTest {

    @Test
    public void testGetFullMethodName() {
        HotMethod hotMethod = new HotMethod("test.package.TestClass", "method1");
        assertEquals("test.package.TestClass.method1", hotMethod.getFullMethodName());
    }

    @Test
    public void testCompareTo() {
        HotMethod hotMethod1 = new HotMethod("test.package.TestClass", "method1", 0, new HashSet<>());
        HotMethod hotMethod2 = new HotMethod("test.package.TestClass", "method2", 1, new HashSet<>());
        HotMethod hotMethod3 = new HotMethod("test.package.TestClass", "method3", 1, new HashSet<>());

        assertEquals(0, hotMethod2.compareTo(hotMethod3));
        assertEquals(-1, hotMethod1.compareTo(hotMethod2));
        assertEquals(1, hotMethod2.compareTo(hotMethod1));
    }

    @Test
    public void testEquals() {
        HotMethod hotMethod1 = new HotMethod("test.package.TestClass", "method1", 0, new HashSet<>());
        HotMethod hotMethod2 = new HotMethod("test.package.TestClass", "method1", 1, new HashSet<>());
        HotMethod hotMethod3 = new HotMethod("test.package.TestClass2", "method1", 1, new HashSet<>());

        assertEquals(hotMethod1, hotMethod2);
        assertEquals(hotMethod2, hotMethod1);
        assertNotEquals(hotMethod3, hotMethod1);
        assertNotEquals(hotMethod3, hotMethod2);
        assertNotEquals(null, hotMethod3);
    }

}
