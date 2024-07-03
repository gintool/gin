package gin.util;

import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import gin.test.UnitTest;
import gin.util.regression.RTSFactory;
import gin.util.regression.RTSStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Profiler for mvn/gradle projects to find the "hot" methods of a test suite
 * using hprof, and to optionally select test cases using a Regression Test
 * Selection (RTS) technique.
 * <p>
 * Run directly from the commandline.
 * <p>
 * Currently it only works with maven projects.
 *
 * @author Giovani
 */
public class RTSProfiler implements Serializable {

    @Serial
    private static final long serialVersionUID = 6763826827126978230L;
    // Constants
    private static final String[] HEADER = {"Project", "MethodIndex", "Method", "Count", "Tests"};
    private static final String PROF_DIR = "profiler_out";
    private static final String JFR_ARG_BEFORE_11 = "-XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:StartFlightRecording=name=Gin,dumponexit=true,settings=profile,filename=";
    private static final String JFR_ARG_11_AFTER = "-XX:+FlightRecorder -XX:StartFlightRecording=name=Gin,dumponexit=true,settings=profile,filename=";
    private static String HPROF_ARG = "-agentlib:hprof=cpu=samples,lineno=y,depth=1,interval=$hprofInterval,file=";
    // Instance Members
    private final File profDir;
    // Commandline arguments
    @Argument(alias = "p", description = "Project name, required", required = true)
    protected String projectName;
    @Argument(alias = "d", description = "Project Directory, reuqired", required = true)
    protected File projectDir;
    @Argument(alias = "o", description = "Results file hot methods")
    protected File outputFile = new File("./profile_results.csv");
    @Argument(alias = "to", description = "Output file for storing the execution time")
    protected File timingOutputFile = new File("profile_timing.csv");
    @Argument(alias = "h", description = "Path to maven bin directory e.g. /usr/local/")
    protected File mavenHome;  // default on OS X
    @Argument(alias = "x", description = "Exclude invocation of profiler, just parse hprof traces.")
    protected Boolean excludeProfiler = false;
    @Argument(alias = "t", description = "Run given maven task rather than test")
    protected String mavenTaskName = "test";
    @Argument(alias = "m", description = "Maven mavenProfile to use, e.g. light-test")
    protected String mavenProfile = "";
    @Argument(alias = "threads", description = "Number of threads to be used by Maven.")
    protected String threads = "1";
    @Argument(alias = "prop", description = "Additional properties to pass to maven. Properties are divided by comma on a 'key=value' format."
            + "For example: \"property1=true,property2=false\"."
            + "If you are using Apache Commons projects, add the \"rat.skip=true\" property, otherwise the projects won't work with Gin.")
    protected String[] additionalProperties = new String[]{};
    @Argument(description = "The Regression Test Selection (RTS) mechanism used to collect test cases for each method. "
            + "Use 'none' for avoiding RTS altogether. "
            + "Available: 'none', 'ekstazi', 'random'. "
            + "Default: 'ekstazi'.")
    protected String rts = "ekstazi";
    @Argument(alias = "hi", description = "Interval for hprof's CPU sampling in milliseconds")
    protected Long hprofInterval = 10L;
    @Argument(alias = "pn", description = "Java profiler file name. If running in parallel, use a different name for each job.")
    protected String profFileName = "java.prof.jfr";
    @Argument(alias = "prof", description = "Profiler to use: jfr or hprof. Default is jfr")
    protected String profilerChoice = "jfr";
    protected Project project;

    public RTSProfiler(String[] args) {
        Args.parseOrExit(this, args);

        project = new Project(projectDir, projectName);
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
        this.profDir = new File(projectDir, PROF_DIR);
        if (this.profilerChoice.equalsIgnoreCase("HPROF")) {
            HPROF_ARG = HPROF_ARG.replace("$hprofInterval", Long.toString(hprofInterval));
        }
        valiateArguments();
    }

    public static void main(String[] args) throws IOException {
        StopWatch watch = StopWatch.createStarted();
        RTSProfiler profiler = new RTSProfiler(args);
        profiler.profile();
        watch.stop();
        profiler.writeTimingResults(watch);
    }

    private void valiateArguments() {
        if (this.project.isGradleProject()
                && this.profilerChoice.trim().equalsIgnoreCase("JFR")
                && SystemUtils.IS_OS_WINDOWS) {
            throw new IllegalArgumentException("Gin will not work with Windows and Java Flight Recorder on Gradle projects.");
        }
    }

    // Main Profile Method
    public void profile() throws IOException {
        Logger.info("Profiling project: " + this.project);
        //Initialise
        Properties properties = new Properties();
        File profFile = FileUtils.getFile(profDir, profFileName);
        // Create RTS strategy if any
        Logger.info("Initialised: " + rts);
        RTSStrategy rtsStrategy = RTSFactory.createRTSStrategy(rts, Paths.get(this.projectDir.getAbsolutePath()).normalize().toString());
        // Execute profiler
        if (!this.excludeProfiler) {
            // Try to create the folder in which the profiling results of hprof
            // will be stored
            try {
                FileUtils.forceMkdir(profDir);
            } catch (IOException ex) {
                Logger.error(ex, "Unable to create profiling folder " + profDir.getAbsolutePath());
                System.exit(-1);
            }
            StringBuilder argLine = new StringBuilder();
            // Inject hprof agent
            String profilerArgumentLine = switch (this.profilerChoice.toUpperCase()) {
                case "HPROF" -> HPROF_ARG;
                default -> JavaUtils.getJavaVersion() < 11 ? JFR_ARG_BEFORE_11 : JFR_ARG_11_AFTER;
            };
            argLine.append(profilerArgumentLine).append(FilenameUtils.normalize(profFile.getAbsolutePath()));

            // Inject the RTS agent (if any)
            String rtsArg = rtsStrategy.getArgumentLine();
            if (!StringUtils.isBlank(argLine)) {
                argLine.append(" ").append(rtsArg);
            }

            // Set the argument line with the agents
            properties.put("argLine", argLine.toString().trim());

            // Set the additional properties
            for (String additionalProperty : additionalProperties) {
                final String[] split = additionalProperty.split("=");
                // If format is valid
                if (split.length == 2) {
                    properties.put(split[0].trim(), split[1].trim());
                }
            }

            // Gets the test goal for the RTS technique
            String rtsTestGoal = rtsStrategy.getTestGoal();
            // If -t was not given (is default), then use the one provided by
            // the RTS technique. Otherwise, use the one sepcified by the user.
            this.mavenTaskName = this.mavenTaskName.equalsIgnoreCase("test")
                    ? rtsTestGoal
                    : this.mavenTaskName;

            // Set the number of threads to be used by Maven
            properties.setProperty("THREADS", threads);

            // Run all test cases
            project.runAllUnitTestsWithProperties(this.mavenTaskName, this.mavenProfile, properties, new String[0]);
        }
        // Collect test results
        Set<UnitTest> allTestCases = project.parseTestReports();
        if (allTestCases.isEmpty()) {
            Logger.error("No tests found in project.");
            System.exit(-1);
        }
        // Get profiled taregt methods
        List<HotMethod> hotMethods = getHotMethods(profFile);
        // Link the test cases to the methods based on the RTS technique
        rtsStrategy.linkTestsToMethods(hotMethods, allTestCases);
        // Order by hotness
        hotMethods.sort(Collections.reverseOrder());
        // Write target method file
        writeResults(hotMethods);
    }

    protected List<HotMethod> getHotMethods(File profFile) throws IOException {
        List<HotMethod> hotMethods = new ArrayList<>();
        Map<String, Integer> methodCounts;
        if (profFile != null && profFile.exists()) {
            if (this.profilerChoice.equalsIgnoreCase("hprof")) {
                methodCounts = Trace.fromHPROFFile(this.project, new UnitTest("", ""), profFile).methodCounts;
            } else {
                methodCounts = Trace.fromJFRFile(this.project, new UnitTest("", ""), profFile).methodCounts;
            }

            hotMethods = methodCounts.entrySet()
                    .stream()
                    .map(traceToHotMethod())
                    .collect(Collectors.toList());
        }
        return hotMethods;
    }

    private Function<Map.Entry<String, Integer>, HotMethod> traceToHotMethod() {
        return entry -> {
            String fullMethodName = entry.getKey();
            int separator = fullMethodName.lastIndexOf(".");
            String className = fullMethodName.substring(0, separator);
            String methodName = fullMethodName.substring(separator + 1);
            return new HotMethod(className, methodName, entry.getValue(), new HashSet<>());
        };
    }

    // Write hot methods to output csv
    private void writeResults(List<HotMethod> hotMethods) {
        CSVWriter writer = null;
        try {
            File parentFile = this.outputFile.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            writer = new CSVWriter(new FileWriter(this.outputFile));
            writer.writeNext(HEADER);

            for (HotMethod hotMethod : hotMethods) {
                String allTestNames = hotMethod.getTests().stream()
                        .map(UnitTest::toString)
                        .collect(Collectors.joining(","));

                String[] row = {this.projectName,
                        Integer.toString(hotMethod.hashCode()),
                        hotMethod.getFullMethodName(),
                        Integer.toString(hotMethod.getCount()),
                        allTestNames
                };

                writer.writeNext(row);
            }
        } catch (IOException e) {
            Logger.error("Error writing hot method file: " + outputFile);
            Logger.trace(e);
            System.exit(-1);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Logger.error("Error closing hot method file: " + outputFile);
                Logger.trace(e);
                System.exit(-1);
            }
        }
    }

    private void writeTimingResults(StopWatch watch) {
        try {
            FileUtils.writeStringToFile(timingOutputFile, watch.getTime() + "\n", Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.error(ex, "Error outputing execution time to: " + timingOutputFile);
        }
    }

}
