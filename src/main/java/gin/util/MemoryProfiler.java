package gin.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.test.UnitTest;

import org.apache.commons.lang3.SystemUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple profiler for mvn/gradle projects to find the "hot" methods of a test suite using hprof.
 * <p>
 * Run directly from the commandline.
 * <p>
 * You provide the project directory, output file, number of reps.... the MemoryProfiler does the rest.
 */
public class MemoryProfiler {

    private static final String[] HEADER = {"Project", "MethodIndex", "Method", "Count", "Tests"};
    private static final String WORKING_DIR = "hprof";
    private static String HPROF_ARG = "-agentlib:hprof=heap=sites,lineno=y,depth=1,interval=$hprofInterval,file=";
    private static String JFR_ARG = "-XX:+FlightRecorder -XX:StartFlightRecording:jdk.ObjectAllocationInNewTLAB#enabled=true,name=Gin,dumponexit=true,settings=profile,filename="; // Could replace ObjectAllocationInNewTLAB with ObjectCount (former is for tmp objects and includes stacktrace)
    
    
    // Instance Members
    private final File workingDir;
    private final Project project;
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
    protected Boolean excludeMemoryProfiler = false;

    // Constants
    @Argument(alias = "s", description = "Skip initial run of all tests, just parse reports. For debugging.")
    protected Boolean skipInitialRun = false;
    @Argument(alias = "n", description = "Only mavenProfile the first n tests. For debugging.")
    protected Integer profileFirstNTests;
    @Argument(alias = "t", description = "Run given maven task rather than test")
    protected String mavenTaskName = "test";
    @Argument(alias = "m", description = "Maven mavenProfile to use, e.g. light-test")
    protected String mavenProfile = "";
    @Argument(alias = "hi", description = "Interval for hprof's CPU sampling in milliseconds")
    protected Long hprofInterval = 1L;
    @Argument(alias = "prof", description = "Profiler to use: JFR or HPROF. Default is JFR")
    protected String profilerChoice = "jfr";
    @Argument(alias = "save", description = "Save individual profiling files, default is delete, set command as 's' to save")
    protected String saveChoice = "d";
    @Argument(alias = "ba", description = "Comma separated list of arguments to pass to Maven or Gradle")
    protected String[] buildToolArgs = new String[0];
    
    public MemoryProfiler(String[] args) {
        Args.parseOrExit(this, args);
        
        int javaVersion = JavaUtils.getJavaVersion();
        if (javaVersion > 9 && javaVersion < 17) {
        	Logger.error("Currently mem profiling only works with Java <=8 or >=17");
        	Logger.error("Current version is " + javaVersion);
        	System.exit(1);
        }

        this.workingDir = new File(projectDir, WORKING_DIR);

        project = new Project(projectDir, projectName);
        if (this.gradleVersion != null) {
            project.setGradleVersion(this.gradleVersion);
        }
        if (this.mavenHome != null) {
            project.setMavenHome(this.mavenHome);
        }
        project.setUp();
        
        if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
	        // Adds the interval provided by the user
	        HPROF_ARG = HPROF_ARG.replace("$hprofInterval", Long.toString(hprofInterval));
        }
        
        valiateArguments();
    }

    public static void main(String[] args) {
        MemoryProfiler profiler = new MemoryProfiler(args);
        profiler.profile();
    }
    
    
    private void valiateArguments() {
        if (this.project.isGradleProject() && this.profilerChoice.trim().equalsIgnoreCase("JFR") && SystemUtils.IS_OS_WINDOWS) {
            throw new IllegalArgumentException("Gin will not work with Windows and Java Flight Recorder on Gradle projects.");
        }
    }
    

    // Main Profile Method

    public void profile() {

        Logger.info("Profiling project: " + this.project);

        if (!this.skipInitialRun) {
            project.runAllUnitTests(this.mavenTaskName, this.mavenProfile, this.buildToolArgs);
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
        if (!this.excludeMemoryProfiler) {
            results = profileTestSuite(tests);
            tests = tests.stream()
                    .filter(test -> results.containsKey(test) && results.get(test).success)
                    .collect(Collectors.toSet());
            reportSummary(results);
        }

        List<MemoryTrace> testMemoryTraces = parseMemoryTraces(tests);

        List<HotMethod> hotMethods = calcHotMethods(testMemoryTraces);

        hotMethods.sort(Collections.reverseOrder());

        writeResults(hotMethods);


    }

    private void reportSummary(Map<UnitTest, ProfileResult> results) {

        Logger.info("Profiling report summary");
        Logger.info("Total number of tests run: " + results.size());

        List<ProfileResult> failures = results.values().stream().filter(result -> !result.success)
                .toList();

        if (!failures.isEmpty()) {
            Logger.warn("Failed to run some tests!");
            Logger.warn(failures.size() + " tests were not executed");
            for (ProfileResult result : failures) {
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
        List<UnitTest> sortedTests = new LinkedList<>(tests);
        Collections.sort(sortedTests);

        for (UnitTest test : sortedTests) {

            if (isParameterizedTest(test)) {
                Logger.warn("Ignoring parameterized test, as jUnit does not support running individual " +
                        "parameterized tests.");
                Logger.warn("See https://github.com/junit-team/junit4/issues/664");
                Logger.warn("Test was: " + test);
                continue;
            }

            testCount++;

            for (int rep = 1; rep <= this.reps; rep++) {

                String args;

                if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
                    args = HPROF_ARG + hprofFile(test, rep).getAbsolutePath();
                } else {
                    args = JFR_ARG + jfrFile(test, rep).getAbsolutePath();
                }
                
                String progressMessage = String.format("Running unit test %s (%d/%d) Rep %d/%d",
                        test, testCount, tests.size(), rep, this.reps);

                Logger.info(progressMessage);

                ProfileResult profileResult;

                try {
                    project.runUnitTest(test, args, this.mavenTaskName, this.mavenProfile, this.buildToolArgs);
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

    private boolean isParameterizedTest(UnitTest test) {
        return test.getMethodName().contains("[");
    }

    protected List<MemoryTrace> parseMemoryTraces(Set<UnitTest> tests) {

        List<MemoryTrace> allMemoryTraces = new LinkedList<>();

        outer:
        for (UnitTest test : tests) {

            List<MemoryTrace> testMemoryTraces = new LinkedList<>();

            if (isParameterizedTest(test)) {
                continue;
            }

            for (int rep = 1; rep <= this.reps; rep++) {

                Logger.info("Parsing trace for test: " + test);

                File traceFile;
                MemoryTrace trace;

                if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
                    traceFile = hprofFile(test, rep);
                    trace = MemoryTrace.fromHPROFFile(this.project, test, traceFile);
                    testMemoryTraces.add(trace);
                } else {
                    traceFile = jfrFile(test, rep);
                    try {
                        trace = MemoryTrace.fromJFRFile(this.project, test, traceFile);
                        testMemoryTraces.add(trace);
                    } catch (IOException e) {
                        Logger.warn("Failed to read JFR file due to IOException: " + e);
                        continue outer;
                    }
                }
                
                
                //delete individual profiling files
                if (saveChoice.equals("d")) {
                    try {
                        Files.deleteIfExists(traceFile.toPath());
                    } catch (IOException e) {
                        Logger.warn("Failed to delete profiling file with IOException: " + e);
                    }
                }
                
            }

            MemoryTrace combinedMemoryTrace = MemoryTrace.mergeMemoryTraces(testMemoryTraces);

            allMemoryTraces.add(combinedMemoryTrace);

        }

        return allMemoryTraces;

    }

    // Parse traces from test suite

    // For each method found in the entire test suite trace, record its overall count, and the name of
    // all tests that called it.
    private List<HotMethod> calcHotMethods(List<MemoryTrace> traces) {

        List<HotMethod> hotMethods = new LinkedList<>();

        MemoryTrace entireTestSuiteMemoryTrace = MemoryTrace.mergeMemoryTraces(traces);

        for (String hotMethod : entireTestSuiteMemoryTrace.allMethods()) {

            Set<UnitTest> callingTests = findTestsCallingMethod(hotMethod, traces);

            HotMethod method = new HotMethod(hotMethod, entireTestSuiteMemoryTrace.getMethodCount(hotMethod), callingTests);

            hotMethods.add(method);

        }

        return hotMethods;

    }

    private Set<UnitTest> findTestsCallingMethod(String method, List<MemoryTrace> traces) {

        Set<UnitTest> tests = new HashSet<>();

        for (MemoryTrace trace : traces) {

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

        for (HotMethod method : hotMethods) {

            List<String> testNames = new LinkedList<>();
            for (UnitTest test : method.tests) {
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
    
    private File jfrFile(UnitTest test, int rep) {
        String testName = test.getTestName();
        String cleanTest = testName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String filename = cleanTest + "_" + rep + ".jfr";
        return new File(workingDir, filename);
    }

    private File hprofFile(UnitTest test, int rep) {
        String testName = test.getTestName();
        String cleanTest = testName.replace(" ", "_");
        String filename = cleanTest + "_" + rep + ".hprof";
        String filenameNoBrackets = filename.replace("()", "");
        return new File(workingDir, filenameNoBrackets);
    }

    private void ensureWorkingDirectory() {

        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }

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

    static class HotMethod implements Comparable<HotMethod> {

        String methodName;
        int count;
        Set<UnitTest> tests;

        HotMethod(String method, int count, Set<UnitTest> tests) {
            this.methodName = method;
            this.count = count;
            this.tests = tests;
        }

        @Override
        public int compareTo(HotMethod o) {
            return Integer.compare(this.count, o.count);
        }

    }

}
