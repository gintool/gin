package gin.util.regression;

import gin.util.regression.impl.EkstaziRTS;
import gin.util.regression.impl.NoneRTS;
import gin.util.regression.impl.RandomRTS;
import gin.util.regression.impl.STARTSRTS;

/**
 * This is a Factory class for the Regression Test Selection (RTS) strategy
 * objects.
 *
 * @author Giovani
 *
 * @see RTSStrategy
 * @see EkstaziRTS
 */
public class RTSFactory {

    /**
     * The identifier of {@link EkstaziRTS}. This should be given as input to
     * the factory method
     * {@link #createRTSStrategy(java.lang.String, java.lang.String) createRTSStrategy}.
     */
    public static final String EKSTAZI = "ekstazi";

    /**
     * The identifier of {@link STARTSRTS}. This should be given as input to the
     * factory method
     * {@link #createRTSStrategy(java.lang.String, java.lang.String) createRTSStrategy}.
     */
    public static final String STARTS = "starts";

    /**
     * The identifier of {@link RandomRTS}. This should be given as input to the
     * factory method
     * {@link #createRTSStrategy(java.lang.String, java.lang.String) createRTSStrategy}.
     */
    public static final String RANDOM = "random";

    /**
     * The identifier of {@link NoneRTS}. This should be given as input to the
     * factory method
     * {@link #createRTSStrategy(java.lang.String, java.lang.String) createRTSStrategy}.
     */
    public static final String NONE = "none";

    /**
     * Creates the RTS technique identified by the {@code rtsName} variable. The
     * available RTS techniques are available in this class as constants.
     *
     * @param rtsName        the RTS object to create
     * @param projectRootDir the root directory of the project under improvement
     * @return the object representing the RTS technique, or {@link NoneRTS} if
     *         unidentified
     *
     * @see #EKSTAZI
     * @see #STARTS
     */
    public static RTSStrategy createRTSStrategy(String rtsName, String projectRootDir) {
        switch (rtsName) {
            case EKSTAZI:
                return new EkstaziRTS(projectRootDir);
            case STARTS:
                return new STARTSRTS(projectRootDir);
            case RANDOM:
                return new RandomRTS();
            case NONE:
            default:
                return new NoneRTS();
        }
    }

}
