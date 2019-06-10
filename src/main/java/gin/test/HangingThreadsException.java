package gin.test;

/**
 * Exception thrown if InternalTestRunner finds a discrepancy in number of running threads before/after a test. Currently unused.
 */
public class HangingThreadsException extends RuntimeException {

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
