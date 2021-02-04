package gin.util.regression;

import gin.util.regression.RTSFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import gin.util.regression.RTSStrategy;
import gin.util.regression.impl.EkstaziRTS;
import gin.util.regression.impl.NoneRTS;
import gin.util.regression.impl.RandomRTS;
import gin.util.regression.impl.STARTSRTS;

/**
 *
 * @author Giovani
 */
public class RTSFactoryTest {

    @Test
    public void testCreateNone() {
        RTSStrategy rts = RTSFactory.createRTSStrategy(RTSFactory.NONE, "");
        assertNotNull(rts);
        assertTrue(rts instanceof NoneRTS);

        rts = RTSFactory.createRTSStrategy("should default to none", "");
        assertNotNull(rts);
        assertTrue(rts instanceof NoneRTS);
    }

    @Test
    public void testCreateEkstazi() {
        RTSStrategy rts = RTSFactory.createRTSStrategy(RTSFactory.EKSTAZI, "");
        assertNotNull(rts);
        assertTrue(rts instanceof EkstaziRTS);
    }

    @Test
    public void testCreateSTARTS() {
        RTSStrategy rts = RTSFactory.createRTSStrategy(RTSFactory.STARTS, "");
        assertNotNull(rts);
        assertTrue(rts instanceof STARTSRTS);
    }

    @Test
    public void testCreateRandom() {
        RTSStrategy rts = RTSFactory.createRTSStrategy(RTSFactory.RANDOM, "");
        assertNotNull(rts);
        assertTrue(rts instanceof RandomRTS);
    }

}
