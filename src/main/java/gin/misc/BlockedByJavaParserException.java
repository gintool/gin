package gin.misc;

import java.io.Serial;

public class BlockedByJavaParserException extends Exception {

    @Serial
    private static final long serialVersionUID = -6294766516508009325L;

    public BlockedByJavaParserException(String message) {
        super(message);
    }

}
