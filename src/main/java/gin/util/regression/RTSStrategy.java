package gin.util.regression;

import gin.test.UnitTest;
import gin.util.HotMethod;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This abstract class defines methods needed to perform Regression Test
 * Selection (RTS) over target classes and test cases during improvement.
 * <p>
 * The methods are implemented with default behaviour, but they can be
 * overriden. Only
 * {@link #getTargetClassesToTestCases(java.util.Collection, java.util.Collection) getTargetClassesToTestCases}
 * is abstract, since this is the real selection of test cases.
 *
 * @author Giovani
 */
public abstract class RTSStrategy implements Serializable {

    @Serial
    private static final long serialVersionUID = 7815012376032989736L;

    /**
     * Gets the argument line to inject the RTS technique with the execution of
     * the program. This can be used for example to run the technique with Maven
     * and Gradle.
     *
     * @return the argument line (if any) to inject the RTS technique during the
     * execution of the program
     */
    public String getArgumentLine() {
        return "";
    }

    /**
     * Gets the test goal to be executed in order to test the software. This
     * goal is the one that will be called by Maven/Gradle on the project.
     *
     * @return the goal to execute with Maven/Gradle. Default is "test".
     */
    public String getTestGoal() {
        return "test";
    }

    /**
     * Links the target methods with the test cases selected by the RTS
     * technique. This method actually performs the selection.
     *
     * @param targetMethods the methods to be improved
     * @param tests         the available unit tests
     */
    public void linkTestsToMethods(Collection<HotMethod> targetMethods, Collection<UnitTest> tests) {
        // Get profiled target classes
        Set<String> targetClasses = targetMethods.stream()
                .map(HotMethod::getClassName)
                .collect(Collectors.toSet());
        Map<String, Set<UnitTest>> results = getTargetClassesToTestCases(targetClasses, tests);
        // Assign test cases to the target methods based on their classes
        targetMethods
                .forEach(hotMethod -> hotMethod.setTests(results.get(hotMethod.getClassName())));
    }

    /**
     * Gets an one to many Map linking the target classes with the test cases
     * selected by the RTS strategy. This is a hook method that actually
     * performs the selection and is called by
     * {@link #linkTestsToMethods(java.util.Collection, java.util.Collection) linkTestsToMethods}.
     *
     * @param targetClasses the classes to be improved
     * @param tests         the available unit tests
     * @return an one to many Map linking the target classes with the test cases
     * selected by the RTS technique
     */
    protected abstract Map<String, Set<UnitTest>> getTargetClassesToTestCases(Collection<String> targetClasses, Collection<UnitTest> tests);

}
