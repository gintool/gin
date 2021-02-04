package gin.util.regression.impl;

import gin.util.HotMethod;
import gin.test.UnitTest;
import java.util.Collection;
import java.util.HashSet;
import gin.util.regression.RTSStrategy;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dummy class representing a retest-all strategy. This class will simply assign
 * all test cases to all methods.
 *
 * @author Giovani
 */
public class NoneRTS extends RTSStrategy {

    @Override
    public void linkTestsToMethods(Collection<HotMethod> targetMethods, Collection<UnitTest> tests) {
        // If no RTS strategy is selected, all target methods use all
        // available test cases.
        targetMethods.stream()
                .forEach(method -> method.setTests(new HashSet<>(tests)));
    }

    /**
     * This should not be called anywhere, but for due diligence, I will
     * implement the expected behaviour which is linking all classes to all test
     * cases.
     */
    @Override
    protected Map<String, Set<UnitTest>> getTargetClassesToTestCases(Collection<String> targetClasses, Collection<UnitTest> tests) {
        return targetClasses.stream()
                .collect(Collectors.toMap(Function.identity(),
                        targetClass -> new HashSet<>(tests)));
    }

}
