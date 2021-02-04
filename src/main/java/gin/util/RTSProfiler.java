package gin.util;

import com.opencsv.CSVWriter;

import java.io.*;
import java.util.*;

import com.sampullara.cli.Argument;
import com.sampullara.cli.Args;

import gin.test.UnitTest;
import gin.util.regression.RTSFactory;
import java.nio.charset.Charset;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.pmw.tinylog.Logger;
import gin.util.regression.RTSStrategy;
import org.apache.commons.lang3.SystemUtils;

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
public class RTSProfiler {

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
            + "WARNING: starts does not work on Windows. "
            + "Use 'none' for avoiding RTS altogether. "
            + "Available: 'none', 'ekstazi', 'starts', 'random'. "
            + "Default: 'ekstazi'.")
    protected String rts = "ekstazi";

    @Argument(alias = "hprof", description = "Java hprof file name. If running in parallel, use a different name for each job.")
    private String hprofFileName = "java.hprof.txt";
    
    @Argument(alias = "hi", description="Interval for hprof's CPU sampling in milliseconds")
    protected Long hprofInterval = 10L;

    // Constants
    private static final String[] HEADER = {"Project", "MethodIndex", "Method", "Count", "Tests"};
    private static final String HPROF_DIR = "hprof";
    private static String HPROF_ARG = "-agentlib:hprof=cpu=samples,lineno=y,depth=1,interval=$hprofInterval,file=";

    // Instance Members
    private File hprofDir;
    private Project project;

    public static void main(String args[]) {
        StopWatch watch = StopWatch.createStarted();
        RTSProfiler profiler = new RTSProfiler(args);
        profiler.profile();
        watch.stop();
        profiler.writeTimingResults(watch);
    }

    public RTSProfiler(String[] args) {
        Args.parseOrExit(this, args);

        if (this.rts.equals(RTSFactory.STARTS)
                && SystemUtils.IS_OS_WINDOWS) {
            // STARTS fails on Windows
            // https://github.com/TestingResearchIllinois/starts/issues/12
            // Although the author claims the tests pass, they actually don't
            throw new IllegalArgumentException("STARTS will not work on Windows. Please, use 'ekstazi' as an alternative.");
        }

        this.hprofDir = new File(projectDir, HPROF_DIR);

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
        
        // Adds the interval provided by the user
        HPROF_ARG = HPROF_ARG.replace("$hprofInterval", Long.toString(hprofInterval));
    }

    // Main Profile Method
    public void profile() {
        Logger.info("Profiling project: " + this.project);
        //Initialise
        Properties properties = new Properties();
        File hprofFile = FileUtils.getFile(hprofDir, hprofFileName);
        // Create RTS strategy if any
        RTSStrategy rtsStrategy = RTSFactory.createRTSStrategy(rts, this.projectDir.getAbsolutePath());
        // Execute profiler
        if (!this.excludeProfiler) {
            // Try to create the folder in which the profiling results of hprof
            // will be stored
            try {
                FileUtils.forceMkdir(hprofDir);
            } catch (IOException ex) {
                Logger.error(ex, "Unable to create hprof folder " + hprofDir.getAbsolutePath());
                System.exit(-1);
            }
            StringBuilder argLine = new StringBuilder();
            // Inject hprof agent
            argLine.append(HPROF_ARG)
                    .append(FilenameUtils.normalize(hprofFile.getAbsolutePath()));

            // Inject the RTS agent (if any)
            String rtsArg = rtsStrategy.getArgumentLine();
            if (rtsArg != null && !rtsArg.trim().isEmpty()) {
                argLine.append(" ").append(rtsArg);
            }

            // Set the argument line witht he agents
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
            this.mavenTaskName = this.mavenTaskName.equals("test")
                    ? rtsTestGoal
                    : this.mavenTaskName;

            // Set the number of threads to be used by Maven
            properties.setProperty("THREADS", threads);

            // Run all test cases
            project.runAllUnitTestsWithProperties(this.mavenTaskName, this.mavenProfile, properties);
        }
        // Collect test results
        Set<UnitTest> allTestCases = project.parseTestReports();
        if (allTestCases.isEmpty()) {
            Logger.error("No tests found in project.");
            System.exit(-1);
        }
        // Get profiled taregt methods
        List<HotMethod> hotMethods = getHotMethods(hprofFile);
        // Link the test cases to the methods based on the RTS technique
        rtsStrategy.linkTestsToMethods(hotMethods, allTestCases);
        // Order by hotness
        Collections.sort(hotMethods, Collections.reverseOrder());
        // Write target method file
        writeResults(hotMethods);
    }

    private List<HotMethod> getHotMethods(File hprofFile) {
        List<HotMethod> hotMethods = new ArrayList<>();
        if (hprofFile != null && hprofFile.exists()) {
            Map<String, Integer> methodCounts = Trace.fromFile(this.project, new UnitTest("", ""), hprofFile).methodCounts;
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
            this.outputFile.getParentFile().mkdirs();
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
            FileUtils.writeStringToFile(timingOutputFile, String.valueOf(watch.getTime()) + "\n", Charset.defaultCharset());
        } catch (IOException ex) {
            Logger.error(ex, "Error outputing execution time to: " + timingOutputFile);
        }
    }

}
