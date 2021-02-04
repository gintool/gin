package gin.util;

import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Giovani
 */
public class HotMethodTest {
    
    @Before
    public void setUp() {
    }

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
        
        assertTrue(hotMethod1.equals(hotMethod2));
        assertTrue(hotMethod2.equals(hotMethod1));
        assertFalse(hotMethod3.equals(hotMethod1));
        assertFalse(hotMethod3.equals(hotMethod2));
        assertFalse(hotMethod3.equals(null));
    }

}
