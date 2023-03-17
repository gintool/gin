package gin.test;

import java.io.Serial;

/**
 * Exception thrown if InternalTestRunner finds a discrepancy in number of running threads before/after a test. Currently unused.
 */
public class HangingThreadsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3285010916124484883L;
    int threadsBefore;
    int threadsAfter;

    public HangingThreadsException(int threadsBefore, int threadsAfter) {
        this.threadsBefore = threadsBefore;
        this.threadsAfter = threadsAfter;
    }

    public String toString() {
        return "Threads before: " + threadsBefore + " != threads after: " + threadsAfter;
    }

}
