package gin.util;

import gin.test.UnitTest;

/*
 * Used by gin.util.Profiler
 */
public class FailedToExecuteTestException extends Exception {

    private Project.BuildType buildType;
    String reason;
    UnitTest test;

    public FailedToExecuteTestException(Project.BuildType buildType, String reason, UnitTest test) {
        this.buildType = buildType;
        this.reason = reason;
        this.test = test;
    }

    public String toString() {
        return this.getClass().getSimpleName() + " " + reason + " for test " + this.test;
    }

}
