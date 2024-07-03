package gin.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.test.UnitTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple profiler for mvn/gradle projects to find the "hot" methods of a test suite using hprof or jfr.
 * <p>
 * Run directly from the commandline.
 * <p>
 * You provide the project directory, output file, number of reps.... the Profiler does the rest.
 */
public class Profiler implements Serializable {

    @Serial
    private static final long serialVersionUID = 766201566071524493L;
    private static final String[] HEADER = {"Project", "MethodIndex", "Method", "Count", "Tests"};
    private static final String WORKING_DIR = "profiler_out";
    private static final String JFR_ARG_BEFORE_11 = "-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=name=Gin,dumponexit=true,settings=profile,filename=";
    private static final String JFR_ARG_11_AFTER = "-XX:+FlightRecorder -XX:StartFlightRecording=name=Gin,dumponexit=true,settings=profile,filename=";
    private static String HPROF_ARG = "-agentlib:hprof=cpu=samples,lineno=y,depth=1,interval=$hprofInterval,file=";
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
    @Argument(alias = "h", description = "Path to maven bin directory e.g. /usr/local/. Leave blank for automatic discovery.")
    protected File mavenHome = null;
    @Argument(alias = "v", description = "Set Gradle version")
    protected String gradleVersion;
    @Argument(alias = "x", description = "Exclude invocation of profiler, just parse hprof traces.")
    protected Boolean excludeProfiler = false;
    @Argument(alias = "s", description = "Skip initial run of all tests, just parse reports. For debugging.")
    protected Boolean skipInitialRun = false;
    @Argument(alias = "n", description = "Only mavenProfile the first n tests. For debugging.")
    protected Integer profileFirstNTests;
    // Constants
    @Argument(alias = "t", description = "Run given maven task rather than test")
    protected String mavenTaskName = "test";
    @Argument(alias = "m", description = "Maven mavenProfile to use, e.g. light-test")
    protected String mavenProfile = "";
    @Argument(alias = "hi", description = "Interval for hprof's CPU sampling in milliseconds")
    protected Long hprofInterval = 10L;
    @Argument(alias = "prof", description = "Profiler to use: JFR or HPROF. Default is JFR")
    protected String profilerChoice = "jfr";
    @Argument(alias = "save", description = "Save individual profiling files, default is delete, set command as 's' to save")
    protected String saveChoice = "d";
    @Argument(alias = "ba", description = "Comma separated list of arguments to pass to Maven or Gradle")
    protected String[] buildToolArgs = new String[0];
        
    public Profiler(String[] args) {
        Args.parseOrExit(this, args);
        this.workingDir = new File(projectDir, WORKING_DIR);

        project = new Project(projectDir, projectName);
        if (this.gradleVersion != null) {
            project.setGradleVersion(this.gradleVersion);
        }
        if (this.mavenHome != null) {
            project.setMavenHome(this.mavenHome);
        } else {
            // If maven home is not set manually, tries to find a home dir in
            // the system variables
            String mavenHomePath = MavenUtils.findMavenHomePath();
            if (mavenHomePath != null) {
                this.mavenHome = FileUtils.getFile(mavenHomePath);
                project.setMavenHome(this.mavenHome);
            }
        }
        project.setUp();
        // Adds the interval provided by the user
        if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
            HPROF_ARG = HPROF_ARG.replace("$hprofInterval", Long.toString(hprofInterval));
        }

        valiateArguments();
        printCommandlineArguments();
    }

    public static void main(String[] args) {
        Profiler profiler = new Profiler(args);
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
        } else {
            Logger.info("Found " + tests.size() + " tests");
        }

        if (this.profileFirstNTests != null) {
            tests = Sets.newHashSet(Iterables.limit(tests, profileFirstNTests));
        }

        Map<UnitTest, ProfileResult> results;
        if (!this.excludeProfiler) {
            results = profileTestSuite(tests);
            tests = tests.stream().filter(test -> results.containsKey(test) && results.get(test).success).collect(Collectors.toSet());
            reportSummary(results);
        }

        Logger.info("Parsing traces for " + tests.size() + " tests");
        List<Trace> testTraces = parseTraces(tests);

        List<HotMethod> hotMethods = calcHotMethods(testTraces);

        hotMethods.sort(Collections.reverseOrder());

        writeResults(hotMethods);


    }

    private void reportSummary(Map<UnitTest, ProfileResult> results) {

        Logger.info("Profiling report summary");
        Logger.info("Total number of tests run: " + results.size());

        List<ProfileResult> failures = results.values().stream().filter(result -> !result.success).toList();

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
                Logger.warn("Ignoring parameterized test, as jUnit does not support running individual " + "parameterized tests.");
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
                    if (JavaUtils.getJavaVersion() < 11) {
                        args = JFR_ARG_BEFORE_11 + jfrFile(test, rep).getAbsolutePath();
                    } else {
                        args = JFR_ARG_11_AFTER + jfrFile(test, rep).getAbsolutePath();
                    }
                }

                String progressMessage = String.format("Running unit test %s (%d/%d) Rep %d/%d", test, testCount, tests.size(), rep, this.reps);

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

    protected List<Trace> parseTraces(Set<UnitTest> tests) {

        List<Trace> allTraces = new LinkedList<>();

        outer:
        for (UnitTest test : tests) {

            List<Trace> testTraces = new LinkedList<>();

            if (isParameterizedTest(test)) {
                continue;
            }

            for (int rep = 1; rep <= this.reps; rep++) {

                Logger.info("Parsing trace for test: " + test);

                File traceFile;
                Trace trace;

                if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
                    traceFile = hprofFile(test, rep);
                    trace = Trace.fromHPROFFile(this.project, test, traceFile);
                    testTraces.add(trace);
                } else {
                    traceFile = jfrFile(test, rep);
                    try {
                        trace = Trace.fromJFRFile(this.project, test, traceFile);
                        testTraces.add(trace);
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

            Trace combinedTrace = Trace.mergeTraces(testTraces);

            allTraces.add(combinedTrace);

        }

        return allTraces;

    }

    // Parse traces from test suite

    // For each method found in the entire test suite trace, record its overall count, and the name of
    // all tests that called it.
    private List<HotMethod> calcHotMethods(List<Trace> traces) {

        List<HotMethod> hotMethods = new LinkedList<>();

        Trace entireTestSuiteTrace = Trace.mergeTraces(traces);

        for (String hotMethod : entireTestSuiteTrace.allMethods()) {

            Set<UnitTest> callingTests = findTestsCallingMethod(hotMethod, traces);

            HotMethod method = new HotMethod(hotMethod, entireTestSuiteTrace.getMethodCount(hotMethod), callingTests);

            hotMethods.add(method);

        }

        return hotMethods;

    }

    private Set<UnitTest> findTestsCallingMethod(String method, List<Trace> traces) {

        Set<UnitTest> tests = new HashSet<>();

        for (Trace trace : traces) {

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

            String[] row = {this.projectName, Integer.toString(hotMethodIndex), method.methodName, Integer.toString(method.count), allTestNames};

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
        String cleanTest = testName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String filename = cleanTest + "_" + rep + ".hprof";
        String filenameNoBrackets = filename.replace("()", "");
        return new File(workingDir, filenameNoBrackets);
    }

    private void ensureWorkingDirectory() {

        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }

    }
    
    private void printCommandlineArguments() {

        try {
            Field[] fields = Profiler.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Argument.class)) {
                    Argument argument = field.getAnnotation(Argument.class);
                    String name = argument.description();
                    Object value = field.get(this);
                    if (value instanceof File) {
                        Logger.info(name + ": " + ((File) value).getPath());
                    } else if (value instanceof String[]) {
                        Logger.info(name + ": " + Arrays.toString((String[])value));
                    } else if (value == null) {
                        Logger.info(name + ": ");
                    } else {
                        Logger.info(name + ": " + value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            Logger.error("Error printing commandline arguments.");
            System.exit(-1);
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
