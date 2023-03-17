package gin.util.regression.impl;

import gin.test.UnitTest;
import gin.util.regression.RTSStrategy;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.io.Serial;
import java.util.*;

/**
 * This class simply selects a random number of test cases at random.
 *
 * @author Giovani
 */
public class RandomRTS extends RTSStrategy {

    @Serial
    private static final long serialVersionUID = -1347083983560579003L;

    /**
     * Random generator for selecting the test cases.
     */
    private final RandomDataGenerator randomGenerator;

    /**
     * Builds this object with the given seed.
     *
     * @param seed random seed
     */
    public RandomRTS(long seed) {
        this.randomGenerator = new RandomDataGenerator(RandomGeneratorFactory.createRandomGenerator(new Random(seed)));
    }

    /**
     * Builds this object with a random seed.
     */
    public RandomRTS() {
        this.randomGenerator = new RandomDataGenerator();
    }

    @Override
    protected Map<String, Set<UnitTest>> getTargetClassesToTestCases(Collection<String> targetClasses, Collection<UnitTest> tests) {
        Map<String, Set<UnitTest>> results = new HashMap<>();
        for (String targetClass : targetClasses) {
            // Copies the list
            List<UnitTest> selectedTests = new ArrayList<>(tests);
            // If the list is not empty
            if (!selectedTests.isEmpty()) {
                // Gets a random number of test cases to select
                int testsToSelect = this.randomGenerator.nextInt(1, tests.size());
                // Shuffles the list
                Collections.shuffle(selectedTests);
                // Selects the first n test cases
                selectedTests = selectedTests.subList(0, testsToSelect);
            }
            // Save the selection
            results.put(targetClass, new HashSet<>(selectedTests));
        }
        return results;
    }

}
