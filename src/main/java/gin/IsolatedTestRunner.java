package gin;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.util.List;

// see https://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836

public class IsolatedTestRunner {

    private static final int REPS = 10;

    /**
     * This method is called using reflection to ensure tests are run in an environment that employs a separate
     * classloader.
     * @param testClasses
     * @throws BuildException
     */
    public TestResult runTestClasses(List<String> testClasses) {

        // Load classes
        Class<?>[] classes = new Class<?>[testClasses.size()];
        for (int i=0; i<testClasses.size(); i++) {
            String test = testClasses.get(i);
            try {
                classes[i] = Class.forName(test);
            } catch (ClassNotFoundException e) {
                String msg = "Unable to find class file for test ["+test+"]. Make sure all " +
                        "tests sources are either included in this test target via a 'src' " +
                        "declaration.";
                System.err.println(msg);
                System.exit(-1);
            }
        }

        // Run
        JUnitCore jUnitCore = new JUnitCore();

        // Run the tests REPS times and calculate the mean via a running average
        double[] elapsed = new double[REPS];
        Result result = null;
        for (int rep=0; rep < elapsed.length; rep++) {
            try {
                long start = System.nanoTime();
                result = jUnitCore.run(classes[0]);
                elapsed[rep] = System.nanoTime() - start;
            } catch (Exception e) {
                System.err.println("Error running junit: " + e);
                System.exit(-1);
            }
        }

        double thirdQuartile = new DescriptiveStatistics(elapsed).getPercentile(75);

        return new TestResult(result, thirdQuartile, true, true, "");


    }

}
