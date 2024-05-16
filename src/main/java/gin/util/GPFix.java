package gin.util;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.Patch;
import gin.test.UnitTest;
import gin.test.UnitTestResult;
import gin.test.UnitTestResultSet;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Method-based GPFix search.
 * Roughly based on: "A systematic study of automated program repair: Fixing 55 out of 105 bugs for $8 each."
 * by Claire Le Goues, Michael Dewey-Vogt, Stephanie Forrest, Westley Weimer (ICSE 2012)
 * and its Java implementation at <a href="https://github.com/squaresLab/genprog4java">...</a>
 */

public class GPFix extends GPSimple {

    @Serial
    private static final long serialVersionUID = 1043876194620277519L;
    // Arguments used in fitness calculation
    private final static double WEIGHT = 2.0;
    @Argument(alias = "rec", description = "Record all fitness values in a HashMap")
    protected Boolean record = false;
    private double multiplier = 0.0;
    private double targetFitness = -1.0;
    private Map<UnitTest, Boolean> testResults = new HashMap<>();
    private Map<Patch, Double> recordedFitness = new HashMap<>();

    public GPFix(String[] args) {
        super(args);
        Args.parseOrExit(this, args);
        printAdditionalArguments();
    }

    // Constructor used for testing
    public GPFix(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    public static void main(String[] args) {
        GPFix sampler = new GPFix(args);
        sampler.sampleMethods();
    }

    private void printAdditionalArguments() {
        Logger.info("Record all fitness values in a HashMap: " + record);
    }

    /*============== Implementation of abstract methods  ==============*/

    protected UnitTestResultSet initFitness(String className, List<UnitTest> tests, Patch origPatch) {

        super.reps = 1;
        Logger.debug("Reset reps, each test to be run only once for fitness calculation.");

        UnitTestResultSet results = testPatch(className, tests, origPatch, null);
        setup(results);
        return results;
    }

    // Calculate fitness
    protected double fitness(UnitTestResultSet results) {

        double fitness = 0;
        Patch patch = results.getPatch();

        if (recordedFitness.containsKey(patch)) {
            return recordedFitness.get(patch);
        }

        if (!results.getCleanCompile()) {
            if (record) {
                recordedFitness.put(patch, fitness);
            }
            return fitness;
        }

        for (UnitTestResult res : results.getResults()) {
            boolean check = this.testResults.get(res.getTest());
            if (res.getPassed()) {
                if (check) {
                    fitness += 1;
                } else {
                    fitness += this.multiplier;
                }
            }
        }
        if (record) {
            recordedFitness.put(patch, fitness);
        }
        if (Double.compare(fitness, this.targetFitness) == 0) {
            Logger.info("Found individual with target fitness: " + patch);
        }
        return fitness;

    }

    // Calculate fitness threshold, for selection to the next generation
    protected boolean fitnessThreshold(UnitTestResultSet results, double orig) {

        return fitness(results) > 0;
    }

    // Compare two fitness values, newFitness better if result > 0
    protected double compareFitness(double newFitness, double oldFitness) {

        return newFitness - oldFitness;
    }

    /*============== Helper method  ==============*/

    // Set multiplier and test data for fitness calculations
    private void setup(UnitTestResultSet results) {

        int passing = 0;
        int failing = 0;
        this.testResults = new HashMap<>();
        this.recordedFitness = new HashMap<>();

        for (UnitTestResult testResult : results.getResults()) {
            if (testResult.getPassed()) {
                this.testResults.put(testResult.getTest(), true);
                passing += 1;
            } else {
                this.testResults.put(testResult.getTest(), false);
                failing += 1;
            }
        }
        this.multiplier = (failing > 0) ? passing * WEIGHT / failing : 0;
        Logger.info("Currently failing tests: " + failing);
        Logger.info("Currently passing tests (i.e., current fitness): " + passing);
        this.targetFitness = passing + (this.multiplier * failing);
        Logger.info("Target fitness: " + this.targetFitness);
    }


} 
