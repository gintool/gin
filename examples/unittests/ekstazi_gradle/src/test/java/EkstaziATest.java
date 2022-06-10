
import static org.junit.Assert.*;

/**
 *
 * @author Giovani
 */
public class EkstaziATest {

    @org.junit.Test
    public void testMethodA1() {
        EkstaziA a = new EkstaziA();
        assertEquals(4, a.methodA());
    }

    @org.junit.Test
    public void testMethodB1() {
        EkstaziA a = new EkstaziA();
        assertEquals(0, a.methodB());
    }
    
}
