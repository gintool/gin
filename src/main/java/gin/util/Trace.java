package gin.util;

import gin.test.UnitTest;
import jdk.jfr.consumer.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Used by gin.util.Profiler.
 */
public class Trace implements Serializable {

    @Serial
    private static final long serialVersionUID = -4519079857156034044L;
    private final UnitTest test;
    Map<String, Integer> methodCounts;

    private Trace(UnitTest test, Map<String, Integer> methodCounts) {
        this.test = test;
        this.methodCounts = methodCounts;
    }

    // Merge traces together, adding method counts where appropriate
    public static Trace mergeTraces(List<Trace> traces) {

        // If all traces are for the same test, merged trace uses that test. Otherwise merged trace has null test.
        UnitTest test = null;
        Set<String> names = traces.stream().map(t -> t.getTest().getTestName()).collect(Collectors.toSet());
        if (!names.contains(null) && names.size() == 1) {
            test = traces.get(0).getTest();
        }

        Map<String, Integer> allSamples = new HashMap<>();

        for (Trace trace : traces) {
            trace.methodCounts.forEach((k, v) -> allSamples.merge(k, v, Integer::sum));
        }

        return new Trace(test, allSamples);

    }

    // Parse a trace from a file
    public static Trace fromHPROFFile(Project project, UnitTest test, File hprofFile) {

        String traceText = "";

        try {
            traceText = FileUtils.readFileToString(hprofFile, "UTF-8");
        } catch (IOException e) {
            Logger.error("Error reading trace file: " + hprofFile);
            Logger.error("One possible cause of this error is if build files are setting the argLine parameter");
            Logger.error("grep through files for \"argLine\" to check.");
            Logger.trace(e);
            System.exit(-1);
        }

        // Details of tracepoint - method and line number, indexed by trace point number.
        Map<Integer, TracePoint> tracePoints = parseTracePoints(traceText);

        // Samples from the table at the end of the file. Use the tracePoints map to add line number.
        Map<String, Integer> methodCounts = parseHPROFMethodCounts(traceText, tracePoints);

        // Finally: clean up methodCounts, to exclude methods not in the project etc.
        Map<String, Integer> cleanedCounts = cleanMethodCounts(project, methodCounts);

        return new Trace(test, cleanedCounts);

    }

    public static Trace fromJFRFile(Project project, UnitTest test, File JFRFile) throws IOException {

        // Samples from the table at the end of the file. Use the tracePoints map to add line number.
        Map<String, Integer> methodCounts = parseJFRMethodCounts(JFRFile, project);

        // Finally: clean up methodCounts, to exclude methods not in the project etc.
        Map<String, Integer> cleanedCounts = cleanMethodCounts(project, methodCounts);

        return new Trace(test, cleanedCounts);

    }

    // Parse hprof file and extract the index that gives number, method, and and line number for a trace point.
    private static Map<Integer, TracePoint> parseTracePoints(String hprof) {

        Map<Integer, TracePoint> tracePoints = new HashMap<>();

        // Here's an example of what we're parsing:
        // TRACE 300938:
        // \tsun.font.CFont.createNativeFont(CFont.java:Unknown line)
        // We're extracting: the trace number, the name of the method, the line number ('Unknown line' special case).

        String regex = "^TRACE (\\d+):(?:\\r\\n|\\r|\\n)^\\t(.*)\\(.*:(.*)\\)$";
        Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher m = p.matcher(hprof);

        while (m.find()) {

            int tracePointNumber = Integer.parseInt(m.group(1));

            String methodName = m.group(2);

            String lineNumberString = m.group(3);

            int lineNumber = -1;
            if (!lineNumberString.equals("Unknown line")) {
                lineNumber = Integer.parseInt(lineNumberString);
            }

            TracePoint tp = new TracePoint(methodName, lineNumber);
            tracePoints.put(tracePointNumber, tp);

        }

        return tracePoints;

    }

    /**
     * Parse the table containing samples, i.e. method names and the number of times seen on the stack.
     * <p>
     * Example of what we're parsing:
     * CPU SAMPLES BEGIN (total = 1324) Sat Sep 29 12:02:19 2018
     * rank   self  accum   count trace method
     * 1  5.36%  5.36%      71 300898 sun.font.CFontManager.loadNativeFonts
     * 2  5.29% 10.65%      70 300465 org.jcodec.codecs.h264.decode.deblock.DeblockingFilter.filterBlockEdgeVert
     * CPU SAMPLES END
     */
    private static Map<String, Integer> parseHPROFMethodCounts(String hprof, Map<Integer, TracePoint> tracePoints) {

        Map<String, Integer> samples = new HashMap<>();

        // Extract table, stripping header and footer

        String header = "rank   self  accum   count trace method$";
        String footer = "CPU SAMPLES END";
        String tableRegex = header + "(.*?)" + footer;

        Pattern p = Pattern.compile(tableRegex, Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = p.matcher(hprof);

        if (m.find()) {
            String table = m.group(1);

            // Iterate over rows, separate by whitespace, get name and count

            BufferedReader bufReader = new BufferedReader(new StringReader(table));
            String row;
            try {
                while ((row = bufReader.readLine()) != null) {
                    if (!row.equals("")) {
                        String[] fields = row.trim().split("\\s+");
                        String methodName = fields[5];
                        int count = Integer.parseInt(fields[3]);
                        int tracePointNumber = Integer.parseInt(fields[4]);
                        int lineNumber = tracePoints.get(tracePointNumber).lineNumber;
                        String fullMethodName = methodName;
                        if (lineNumber != -1) {
                            fullMethodName += ":" + lineNumber;
                        }
                        samples.put(fullMethodName, count);
                    }
                }
            } catch (IOException e) {
                Logger.error("Error reading enumerate table from hprof file.");
                Logger.trace(e);
                System.exit(-1);
            }
        }

        return samples;

    }

    private static Map<String, Integer> parseJFRMethodCounts(File jfrF, Project project) throws IOException {

        Map<String, Integer> samples = new HashMap<>();

        //use main classes to find methods in the main program
        Set<String> mainClasses = project.allMainClasses();

        try (RecordingFile jfr = new RecordingFile(Paths.get(jfrF.getAbsolutePath()))) {

            //read all events from the JFR profiling file
            while (jfr.hasMoreEvents()) {
                RecordedEvent event = jfr.readEvent();
                String check = event.getEventType().getName();
//System.out.println("******" + check);
                //if this event is an exectution sample, it will contain a call stack snapshot
                if (check.endsWith("jdk.ExecutionSample")) { // com.oracle.jdk.ExecutionSample for Oracle JDK, jdk.ExecutionSample for OpenJDK
                    RecordedStackTrace s = event.getStackTrace();

                    if (s != null) {

                        //traverse the call stack, if a frame is part of the main program,
                        //return it
                        for (int i = 0; i < s.getFrames().size(); i++) {

                            RecordedFrame topFrame = s.getFrames().get(i);
                            RecordedMethod method = topFrame.getMethod();

                            String methodName = method.getType().getName();
                            String className = StringUtils.substringBeforeLast(methodName, ".");

                            if (mainClasses.contains(methodName) || mainClasses.contains(className)) {
                                methodName += "." + method.getName() + ":" + topFrame.getLineNumber();
                                samples.merge(methodName, 1, Integer::sum);
                                break;
                            }
                        }


                    }
                }
            }
            return samples;

        }

    }

    // Run through method counts and cleanup
    private static Map<String, Integer> cleanMethodCounts(Project project, Map<String, Integer> methodCounts) {

        Map<String, Integer> cleanTrace = new HashMap<>();

        // Get all classes in this project
        Set<String> mainClasses = project.allMainClasses();
        Set<String> testClasses = project.allTestClasses();

        for (Map.Entry<String, Integer> entry : methodCounts.entrySet()) {

            String method = entry.getKey();
            String className = StringUtils.substringBeforeLast(method, ".");

            boolean includeMethod = shouldIncludeMethod(method);

            // Check if belongs to this project
            boolean classInMain = mainClasses.contains(className);
            boolean classInTest = testClasses.contains(className);

            boolean hasLineNumber = entry.getKey().contains(":");

            if (classInMain && includeMethod && hasLineNumber) {

                String lineRegex = "^(.*):(\\d*)";
                Pattern linePattern = Pattern.compile(lineRegex);
                Matcher lineMatcher = linePattern.matcher(entry.getKey());
                lineMatcher.find();

                String methodName = lineMatcher.group(1);
                int lineNumber = Integer.parseInt(lineMatcher.group(2));

                String fullMethodName = project.getMethodSignature(methodName, lineNumber);

                // If we can find the original method (we may not, e.g. interface overridden)
                if (fullMethodName == null) {
                    Logger.warn("Excluding method as class in main tree but method not found: " + method);
                    if (method.contains(".values")) {
                        Logger.warn("This is likely because the method relates to an enum type.");
                    }
                } else {
                    cleanTrace.merge(fullMethodName, entry.getValue(), Integer::sum);
                }

            } else {

                if (!includeMethod) {

                    Logger.info("Excluding method because exceptional case (inner class etc.): " + method);

                } else if (classInTest) {

                    Logger.info("Excluding method because class is a test class: " + method);

                } else if (!hasLineNumber) {

                    Logger.info("Excluding method because hprof gave no line number: " + method);

                } else if (method.contains(project.getProjectName())) {

                    Logger.warn("Excluding method because not in main project tree: " + method);
                    Logger.warn(" ...but the method contains the project name! Possibly a bug.");

                } else {

                    Logger.info("Excluding method because not in main project tree: " + method);

                }

            }

        }

        return cleanTrace;

    }

    private static boolean shouldIncludeMethod(String method) {

        if (method.contains("$")) {
            return false;
        }

        if (method.contains("<init>")) {
            return false;
        }

        return !method.contains("clinit");

    }

    public Set<String> allMethods() {
        return methodCounts.keySet();
    }

    public int getMethodCount(String method) {
        return this.methodCounts.get(method);
    }

    public UnitTest getTest() {
        return test;
    }

    static class TracePoint {

        String method;
        int lineNumber;  // -1 = unknown

        public TracePoint(String method, int line) {
            this.method = method;
            this.lineNumber = line;
        }

    }

}
