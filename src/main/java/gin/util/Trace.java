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

    public static Trace fromJFRFile(Project project, UnitTest test, File JFRFile) throws IOException {

        // Samples from the table at the end of the file. Use the tracePoints map to add line number.
        Map<String, Integer> methodCounts = parseJFRMethodCounts(JFRFile, project);

        // Finally: clean up methodCounts, to exclude methods not in the project etc.
        Map<String, Integer> cleanedCounts = cleanMethodCounts(project, methodCounts);

        return new Trace(test, cleanedCounts);

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

                    Logger.info("Excluding method because jfr gave no line number: " + method);

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
