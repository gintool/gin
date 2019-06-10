package gin.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sampullara.cli.Argument;
import com.sampullara.cli.Args;

import gin.test.UnitTest;
import org.pmw.tinylog.Logger;

/**
 * Simple profiler for mvn/gradle projects to find the "hot" methods of a test suite using hprof.
 *
 * Run directly from the commandline.
 *
 * You provide the project directory, output file, number of reps.... the Profiler does the rest.
 */
public class Profiler {

    // Commandline arguments
    @Argument(alias = "p", description = "Project name, required", required = true)
    protected String projectName;

    @Argument(alias = "d", description = "Project Directory, reuqired", required = true)
    protected File projectDir;

    @Argument(alias = "o", description = "Results file hot methods")
    protected File outputFile = new File("profile_results.csv");

    @Argument(alias = "r", description = "Number of times to run each test")
    protected Integer reps = 1;

    @Argument(alias = "h", description = "Path to maven bin directory e.g. /usr/local/")
    protected File mavenHome = new File("/usr/local/");  // default on OS X

    @Argument(alias = "v", description = "Set Gradle version")
    protected String gradleVersion;

    @Argument(alias = "x", description = "Exclude invocation of profiler, just parse hprof traces.")
    protected Boolean excludeProfiler = false;

    @Argument(alias = "s", description = "Skip initial run of all tests, just parse reports. For debugging.")
    protected Boolean skipInitialRun = false;

    @Argument(alias = "n", description = "Only mavenProfile the first n tests. For debugging.")
    protected Integer profileFirstNTests;

    @Argument(alias = "t", description = "Run given maven task rather than test")
    protected String mavenTaskName = "test";

    @Argument(alias = "m", description="Maven mavenProfile to use, e.g. light-test")
    protected String mavenProfile = "";



    // Constants

    private static final String[] HEADER = {"Project", "MethodIndex", "Method", "Count", "Tests"};
    private static final String WORKING_DIR = "hprof";
    private static final String HPROF_ARG = "-agentlib:hprof=cpu=samples,lineno=y,depth=1,file=";

    // Instance Members
    private File workingDir;
    private Project project;

    public static void main(String args[]) {
        Profiler profiler = new Profiler(args);
        profiler.profile();
    }

    public Profiler(String[] args) {
        Args.parseOrExit(this, args);

        this.workingDir = new File(projectDir, WORKING_DIR);

        project = new Project(projectDir, projectName);
        if (this.gradleVersion != null) {
            project.setGradleVersion(this.gradleVersion);
        }
        if (this.mavenHome != null) {
            project.setMavenHome(this.mavenHome);
        }
    }

    // Main Profile Method

    public void profile() {

        Logger.info("Profiling project: " + this.project);

        if (!this.skipInitialRun) {
            project.runAllUnitTests(this.mavenTaskName, this.mavenProfile);
        }

        Set<UnitTest> tests = project.parseTestReports();

        if (tests.isEmpty()) {
            Logger.error("No tests found in project.");
            System.exit(-1);
        }

        if (this.profileFirstNTests != null) {
            tests = Sets.newHashSet(Iterables.limit(tests, profileFirstNTests));
        }

        Map<UnitTest, ProfileResult> results;
        if (!this.excludeProfiler) {
            results = profileTestSuite(tests);
            tests = tests.stream()
                        .filter(test -> results.keySet().contains(test) && results.get(test).success)
                        .collect(Collectors.toSet());
            reportSummary(results);
        }

        List<Trace> testTraces = parseTraces(tests);

        List<HotMethod> hotMethods = calcHotMethods(testTraces);

        Collections.sort(hotMethods, Collections.reverseOrder());

        writeResults(hotMethods);


    }

    private void reportSummary(Map<UnitTest, ProfileResult> results) {

        Logger.info("Profiling report summary");
        Logger.info("Total number of tests run: " + results.size());

        List<ProfileResult> failures = results.values().stream().filter(result -> !result.success)
                .collect(Collectors.toList());

        if (failures.size() != 0) {
            Logger.warn("Failed to run some tests!");
            Logger.warn(failures.size() + " tests were not executed");
            for (ProfileResult result: failures) {
                Logger.warn("Failed to run test: " + result.test + " due to exception: " + result.exception);
            }
        } else {
            Logger.info("All tests were executed.");
        }

    }


    // Run entire test suite, one test at a time, with hprof enabled

    protected Map<UnitTest, ProfileResult> profileTestSuite(Set<UnitTest> tests) {

        Map<UnitTest, ProfileResult> results = new HashMap<>();

        ensureWorkingDirectory();

        int testCount = 0;

        // Sort for replication when debugging
        List<UnitTest> sortedTests = new LinkedList(tests);
        Collections.sort(sortedTests);

        for (UnitTest test: sortedTests) {

            if (isParameterizedTest(test)) {
                Logger.warn("Ignoring parameterized test, as jUnit does not support running individual " +
                        "parameterized tests.");
                Logger.warn("See https://github.com/junit-team/junit4/issues/664");
                Logger.warn("Test was: " + test);
                continue;
            }

            testCount++;

            for (int rep=1; rep <= this.reps; rep++) {

                String args = HPROF_ARG + hprofFile(test, rep).getAbsolutePath();

                String progressMessage = String.format("Running unit test %s (%d/%d) Rep %d/%d",
                                                        test, testCount, tests.size(), rep, this.reps);

                Logger.info(progressMessage);

                ProfileResult profileResult;

                try {
                    project.runUnitTest(test, args, this.mavenTaskName, this.mavenProfile);
                    profileResult = new ProfileResult(test, true, null);
                } catch (FailedToExecuteTestException e) {
                    Logger.warn("Failed to execute test: " + test + " due to Exception: " + e);
                    profileResult = new ProfileResult(test, false, e);
                }

                results.put(test, profileResult);

            }

        }

        return results;

    }

    static class ProfileResult {
        UnitTest test;
        boolean success;
        Exception exception;
        ProfileResult(UnitTest test, boolean success, Exception exception) {
            this.test = test;
            this.success = success;
            this.exception = exception;
        }
    }

    private boolean isParameterizedTest(UnitTest test) {
        return test.getMethodName().contains("[");
    }

    // Parse traces from test suite

    protected List<Trace> parseTraces(Set<UnitTest> tests) {

        List<Trace> allTraces = new LinkedList<>();

        for (UnitTest test: tests) {

            List<Trace> testTraces = new LinkedList<>();

            if (isParameterizedTest(test)) {
                continue;
            }

            for (int rep=1; rep <= this.reps; rep++) {

                Logger.info("Parsing trace for test: " + test);

                File traceFile = hprofFile(test, rep);
                Trace trace = Trace.fromFile(this.project, test, traceFile);
                testTraces.add(trace);

            }

            Trace combinedTrace = Trace.mergeTraces(testTraces);

            allTraces.add(combinedTrace);

        }

        return allTraces;

    }

    // For each method found in the entire test suite trace, record its overall count, and the name of
    // all tests that called it.
    private List<HotMethod> calcHotMethods(List<Trace> traces) {

        List<HotMethod> hotMethods = new LinkedList<>();

        Trace entireTestSuiteTrace = Trace.mergeTraces(traces);

        for (String hotMethod: entireTestSuiteTrace.allMethods()) {

            Set<UnitTest> callingTests = findTestsCallingMethod(hotMethod, traces);

            HotMethod method = new HotMethod(hotMethod, entireTestSuiteTrace.getMethodCount(hotMethod), callingTests);

            hotMethods.add(method);

        }

        return hotMethods;

    }

    private Set<UnitTest> findTestsCallingMethod(String method, List<Trace> traces) {

        Set<UnitTest> tests = new HashSet<>();

        for (Trace trace: traces) {

            if (trace.allMethods().contains(method)) {
                tests.add(trace.getTest());
            }

        }

        return tests;

    }

    // Write hot methods to output csv
    private void writeResults(List<HotMethod> hotMethods) {

        CSVWriter writer = null;

        try {
            writer = new CSVWriter(new FileWriter(this.outputFile));
        } catch (IOException e) {
            Logger.error("Error writing hot method file: " + outputFile);
            Logger.trace(e);
            System.exit(-1);
        }

        writer.writeNext(HEADER);

        int hotMethodIndex = 1;

        for (HotMethod method: hotMethods) {

            List<String> testNames = new LinkedList<>();
            for (UnitTest test: method.tests) {
                testNames.add(test.toString());
            }
            String allTestNames = String.join(",", testNames);

            String[] row = {this.projectName,
                            Integer.toString(hotMethodIndex),
                            method.methodName,
                            Integer.toString(method.count),
                            allTestNames
                            };

            writer.writeNext(row);

            hotMethodIndex++;

        }

        try {
            writer.close();
        } catch (IOException e) {
            Logger.error("Error closing hot method file: " + outputFile);
            Logger.trace(e);
            System.exit(-1);
        }

    }

    class HotMethod implements Comparable<HotMethod> {

        HotMethod(String method, int count, Set<UnitTest> tests) {
            this.methodName = method;
            this.count = count;
            this.tests = tests;
        }


        String methodName;
        int count;
        Set<UnitTest> tests;

        @Override
        public int compareTo(HotMethod o) {
            return Integer.compare(this.count, o.count);
        }

    }

    private File hprofFile(UnitTest test, int rep) {
        String testName = test.getTestName();
        String cleanTest = testName.replace(" ", "_");
        String filename = cleanTest + "_" + rep + ".hprof";
        String filenameNoBrackets = filename.replace("()", "");
        File hprof = new File(workingDir, filenameNoBrackets);
        return  hprof;
    }

    private void ensureWorkingDirectory() {

        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }

    }

}
