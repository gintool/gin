
import static org.junit.Assert.*;

/**
 *
 * @author Giovani
 */
public class EkstaziBTest {

    @org.junit.Test
    public void testSum1() {
        EkstaziB b = new EkstaziB();
        assertEquals(3, b.sum(1, 2));
    }

    @org.junit.Test
    public void testSum2() {
        EkstaziB b = new EkstaziB();
        assertEquals(5, b.sum(2, 3));
    }

    @org.junit.Test
    public void testSum3() {
        EkstaziB b = new EkstaziB();
        assertEquals(10, b.sum(2, 8));
    }

}
