package gin.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.test.UnitTest;
import gin.util.enums.ProfilerChoice;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//    private static final String JFR_ARG_11_AFTER = "-Xlog:jfr+system=info -XX:+FlightRecorder -XX:FlightRecorderOptions=stackdepth=256 -XX:StartFlightRecording=name=Gin#JFRNAME#,settings=gin-profile.jfc,dumponexit=true,settings=profile,delay=1s,filename=#JFRNAME#";
    private static final String JFR_ARG_11_AFTER = "-Xlog:jfr+system=info -XX:+FlightRecorder -XX:FlightRecorderOptions=stackdepth=256 -XX:StartFlightRecording=name=Gin#JFRNAME#,settings=#SETTINGSNAME#,dumponexit=true,settings=profile,duration=12s,filename=#JFRNAME#";
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
    protected String profilerChoice = String.valueOf(ProfilerChoice.JFR);
    @Argument(alias = "save", description = "Save individual profiling files, default is delete, set command as 's' to save")
    protected boolean saveProfiles = false;
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

        Map<UnitTest, List<ProfileResult>> results = null;
        if (!this.excludeProfiler) {
            results = profileTestSuite(tests);
            //tests = tests.stream().filter(test -> results.containsKey(test) && results.get(test).success).collect(Collectors.toSet());
            reportSummary(results);
        } else {
            // Build purely from disk (optionally pass a timestamp to filter)
            results = buildResultsFromExistingFiles(tests /*, someTimestampOrNull */);
        }

        Logger.info("Parsing traces for " + tests.size() + " tests");
        List<Trace> testTraces = parseTraces(results.values());

        List<HotMethod> hotMethods = calcHotMethods(testTraces);

        hotMethods.sort(Collections.reverseOrder());

        writeResults(hotMethods);


    }

    private void reportSummary(Map<UnitTest, List<ProfileResult>> results) {

        Logger.info("Profiling report summary");
        Logger.info("Total number of tests run: " + results.values().stream()
                .mapToLong(List::size)
                .sum());

        //List<ProfileResult> failures = results.values().stream().filter(result -> !result.success).toList();
        List<ProfileResult> failures = results.values().stream()
                .flatMap(List::stream)        // flatten List<ProfileResult> into a stream of ProfileResult
                .filter(result -> !result.success)
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

    protected Map<UnitTest, List<ProfileResult>> profileTestSuite(Set<UnitTest> tests) {

        Map<UnitTest, List<ProfileResult>> results = new HashMap<>();

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

            List<ProfileResult> profileResultsForThisTest = new ArrayList<>(this.reps);
            for (int rep = 1; rep <= this.reps; rep++) {

                String args;

                long startTime = System.currentTimeMillis(); // this is to give each JFR output a unique filename

                if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
                    args = HPROF_ARG + hprofFile(test, rep).getAbsolutePath();
                } else {
                    if (JavaUtils.getJavaVersion() < 11) {
                        args = JFR_ARG_BEFORE_11 + jfrFile(test, rep, startTime).getAbsolutePath();
                    } else {
                        args = JFR_ARG_11_AFTER.replace("#JFRNAME#", jfrFile(test, rep, startTime).getAbsolutePath()).replace("#SETTINGSNAME#", writeJfrConfigNextToOutputs(workingDir).toString());
                    }
                }


                String progressMessage = String.format("Running unit test %s (%d/%d) Rep %d/%d", test, testCount, tests.size(), rep, this.reps);

                Logger.info(progressMessage);

                ProfileResult profileResult;

                try {
                    project.runUnitTest(test, args, this.mavenTaskName, this.mavenProfile, this.buildToolArgs);
                    profileResult = new ProfileResult(test, true, null, rep, startTime, jfrFile(test, rep, startTime).getAbsolutePath());

                    // Optional: wait briefly to ensure the dump completed before reading
                    try {
                        java.nio.file.Path p = jfrFile(test, rep, startTime).toPath();
                        long s1 = java.nio.file.Files.size(p);
                        Thread.sleep(200);
                        long s2 = java.nio.file.Files.size(p);
                        if (s1 != s2) Thread.sleep(500);
                    } catch (Exception ignore) {}
                } catch (FailedToExecuteTestException e) {
                    Logger.warn("Failed to execute test: " + test + " due to Exception: " + e);
                    profileResult = new ProfileResult(test, false, e, rep, startTime, jfrFile(test, rep, startTime).getAbsolutePath());
                }

                profileResultsForThisTest.add(profileResult);

            }

            results.put(test, profileResultsForThisTest);

        }

        return results;

    }

    private boolean isParameterizedTest(UnitTest test) {
        return test.getMethodName().contains("[");
    }

    protected List<Trace> parseTraces(/*Set<UnitTest> tests*/ Collection<List<ProfileResult>> results) {

        List<Trace> allTraces = new LinkedList<>();

        outer:
        for (List<ProfileResult> results2 : results) {
            for (ProfileResult result : results2) {
                Logger.info("Parsing..." + result.filename);
                // skip if test failed
                if (!result.success) {
                    Logger.info("Skipped");
                    continue outer;
                }
                Logger.info("Processing");

                UnitTest test = result.test;

                List<Trace> testTraces = new LinkedList<>();

                if (isParameterizedTest(test)) {
                    continue;
                }

//              for (int rep = 1; rep <= this.reps; rep++) { // no need to repeat, there's a result file per repeat!

                Logger.info("Parsing trace for test: " + test);

                File traceFile;
                Trace trace;

                if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
                    traceFile = hprofFile(test, result.rep);
                    trace = Trace.fromHPROFFile(this.project, test, traceFile);
                    testTraces.add(trace);
                } else {
                    traceFile = jfrFile(test, result.rep, result.startTime);
                    try {
                        trace = Trace.fromJFRFile(this.project, test, traceFile);
                        testTraces.add(trace);
                    } catch (IOException e) {
                        Logger.warn("Failed to read JFR file due to IOException: " + e);
                        Logger.warn(e);
                        continue outer;
                    }


                }

                //delete individual profiling files
                if (!saveProfiles) {
                    try {
                        Files.deleteIfExists(traceFile.toPath());
                    } catch (IOException e) {
                        Logger.warn("Failed to delete profiling file with IOException: " + e);
                    }
                }

//            }


                Trace combinedTrace = Trace.mergeTraces(testTraces);

                allTraces.add(combinedTrace);
            }

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

    private File jfrFile(UnitTest test, int rep, long startTime) {
        String testName = test.getTestName();
        String cleanTest = testName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String filename = cleanTest + "_" + rep + "_" + startTime + ".jfr";
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

    static Path writeJfrConfigNextToOutputs(File projectDir) {
        String res = "gin-profile.jfc";
        try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(res)) {
            if (in == null) throw new FileNotFoundException("Missing resource: " + res);
            Path dir = new File(projectDir, "jfr-config").toPath();
            Files.createDirectories(dir);
            Path out = dir.resolve("gin-profile.jfc");
            Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return out.toAbsolutePath();
        } catch (IOException e) {
            Logger.error("Couldn't move JFR config file to working directory. Exception was:");
            Logger.error(e);
            System.exit(1);
            return null;
        }
    }

    /**
     * Rebuild results by scanning profiler_out for JFR files produced earlier.
     * Matches files named: <sanitizedTest>_<rep>_<startTime>.jfr
     * (same scheme as jfrFile(...))
     */
    protected Map<UnitTest, List<ProfileResult>> buildResultsFromExistingFiles(Set<UnitTest> tests) {
        boolean wantJfr   = this.profilerChoice.equalsIgnoreCase("JFR");
        boolean wantHprof = this.profilerChoice.equalsIgnoreCase("HPROF");
        return buildResultsFromExistingFiles(tests, wantJfr, wantHprof, null);
    }

    /**
     * Variant that filters by exact startTime (ms since epoch). If timestampFilter is null, returns all.
     */
    protected Map<UnitTest, List<ProfileResult>> buildResultsFromExistingFiles(Set<UnitTest> tests, boolean includeJfr, boolean includeHprof, Long timestampFilter) {

        Map<UnitTest, List<ProfileResult>> out = new HashMap<>();

        if (workingDir == null || !workingDir.isDirectory()) {
            Logger.warn("Working directory not found: " + workingDir + " — returning empty results.");
            return out;
        }

        // Precompute sanitized test name -> UnitTest for quick lookup.
        Map<String, UnitTest> bySanitized = new HashMap<>(tests.size());
        for (UnitTest t : tests) {
            // Skip parameterized tests, consistent with runtime behaviour.
            if (isParameterizedTest(t)) continue;
            bySanitized.put(sanitizeForJfr(t.getTestName()), t); // class.method  -> sanitized
        }

        // Files we care about: *.jfr
        File[] files = workingDir.listFiles((dir, name) ->
                (includeJfr   && name.endsWith(".jfr"))   ||
                        (includeHprof && name.endsWith(".hprof")));
        if (files == null || files.length == 0) return out;

        //   JFR:   <sanitized>_<rep>_<millis>.jfr
        //   HPROF: <sanitized>_<rep>.hprof
        Pattern JFR  = Pattern.compile("^(?<t>.+)_(?<rep>\\d+)_(?<ts>\\d{10,})\\.jfr$");
        Pattern HPROF= Pattern.compile("^(?<t>.+)_(?<rep>\\d+)\\.hprof$");

        for (File f : files) {
            String name = f.getName();

            // Try JFR first
            if (includeJfr) {
                Matcher mj = JFR.matcher(name);
                if (mj.matches()) {
                    String sanitized = mj.group("t");
                    int rep; long ts;
                    try {
                        rep = Integer.parseInt(mj.group("rep"));
                        ts  = Long.parseLong(mj.group("ts"));
                    } catch (NumberFormatException nfe) {
                        continue;
                    }
                    if (timestampFilter != null && ts != timestampFilter) continue;

                    UnitTest test = bySanitized.get(sanitized);
                    if (test == null) continue;

                    ProfileResult pr = new ProfileResult(
                            test,
                            true,
                            null,
                            rep,
                            ts,
                            f.getAbsolutePath()
                    );
                    out.computeIfAbsent(test, k -> new ArrayList<>()).add(pr);
                    continue;
                }
            }

            // Try HPROF
            if (includeHprof) {
                Matcher mh = HPROF.matcher(name);
                if (mh.matches()) {
                    String sanitized = mh.group("t");
                    int rep;
                    try {
                        rep = Integer.parseInt(mh.group("rep"));
                    } catch (NumberFormatException nfe) {
                        continue;
                    }
                    UnitTest test = bySanitized.get(sanitized);
                    if (test == null) continue;

                    // startTime is unused for HPROF in parseTraces(); set lastModified for traceability.
                    long ts = f.lastModified();

                    ProfileResult pr = new ProfileResult(
                            test,
                            true,
                            null,
                            rep,
                            ts,
                            f.getAbsolutePath()
                    );
                    out.computeIfAbsent(test, k -> new ArrayList<>()).add(pr);
                }
            }
        }

        // Stable ordering
        for (List<ProfileResult> list : out.values()) {
            list.sort(Comparator
                    .comparingInt((ProfileResult r) -> r.rep)
                    .thenComparingLong(r -> r.startTime));
        }

        return out;
    }

    private static String sanitizeForJfr(String testName) {
        // Must match the logic in jfrFile(UnitTest,int,long)
        return testName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_"); // keep dot and dash only
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
        int rep;
        long startTime;
        String filename;

        ProfileResult(UnitTest test, boolean success, Exception exception, int rep, long startTime, String filename) {
            this.test = test;
            this.success = success;
            this.exception = exception;
            this.rep = rep;
            this.startTime = startTime;
            this.filename = filename;
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
