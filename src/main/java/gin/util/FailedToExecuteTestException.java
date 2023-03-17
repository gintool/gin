package gin.util;

import gin.test.UnitTest;

import java.io.Serial;

/*
 * Used by gin.util.Profiler
 */
public class FailedToExecuteTestException extends Exception {

    @Serial
    private static final long serialVersionUID = -8186678933926975283L;
    private final Project.BuildType buildType;
    String reason;
    UnitTest test;

    public FailedToExecuteTestException(Project.BuildType buildType, String reason, UnitTest test) {
        this.buildType = buildType;
        this.reason = reason;
        this.test = test;
    }

    public String toString() {
        return "Build failure type " + buildType + " with class " + this.getClass().getSimpleName() + " " + reason + " for test " + this.test;
    }

}
