package gin.test.regression.impl;

import gin.test.regression.RTSFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import gin.test.regression.RTSStrategy;

/**
 *
 * @author Giovani
 */
public class RTSFactoryTest {

    @Test
    public void testCreateRegressionTestSelection() {
        RTSStrategy rts = RTSFactory.createRTSStrategy("none", "");
        assertTrue(rts instanceof NoneRTS);
        rts = RTSFactory.createRTSStrategy(RTSFactory.EKSTAZI, "");
        assertNotNull(rts);
        assertTrue(rts instanceof EkstaziRTS);
        rts = RTSFactory.createRTSStrategy(RTSFactory.STARTS, "");
        assertNotNull(rts);
        assertTrue(rts instanceof STARTSRTS);
    }

}
