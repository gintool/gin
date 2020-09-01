package mypackage;

import static org.junit.Assert.assertFalse;
import org.junit.Test;

/**
 * Test class to test test poisoning over multiple repetitions.
 *
 * @author Giovani
 */
public class Poison {

    public static boolean POISONED = false;

    @Test
    public void testPoison() {
        assertFalse(POISONED);
        POISONED = true;
    }

}
