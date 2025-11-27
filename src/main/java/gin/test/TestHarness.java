package gin.test;

import com.sampullara.cli.Args;

import edu.emory.mathcs.backport.java.util.Arrays;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
import org.junit.platform.launcher.EngineFilter;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.Request;
import org.junit.runner.notification.Failure;

/**
 * Runs a given test request. Uses sockets to communicate with ExternalTestRunner.
 */
public class TestHarness implements Serializable {

    public static final String PORT_PREFIX = "PORT";
    @Serial
    private static final long serialVersionUID = -6547478455821943382L;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public TestHarness(String[] args) {
        Args.parseOrExit(this, args);
        start();
    }

    public static void main(String[] args) {
        new TestHarness(args);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            System.out.println(PORT_PREFIX + "=" + port); // tell the ExternalTestRunner what port we'll be using

            clientSocket = serverSocket.accept();
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),
                    java.nio.charset.StandardCharsets.UTF_8), true);
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),
                    java.nio.charset.StandardCharsets.UTF_8));
            String command;
            while ((command = in.readLine()) != null) {
                command = command.trim();
                if (command.isEmpty()) {
                    continue; // ignore keep-alives/blank lines
                }
                if ("stop".equalsIgnoreCase(command)) {
                    // optional: let the parent know we’re stopping
                    out.println("ok");   // autoFlush should be true
                    out.flush();
                    break;               // exit the loop -> stop()
                }

                try {
                    String response = runTest(command);

                    out.println((response != null ? cleanForSocket(response) : "crash,unknown,Unknown,0,false,0,0,gin.test.TestHarness$NoResponse"));
                } catch (Throwable t) {
                    // return a synthetic CSV line the parent can parse
                    out.println("crash,unknown,Unknown,0,false,0,0," + t.getClass().getName());
                    //t.printStackTrace(System.err);
                }
            }
            stop();

        } catch (IOException e) {
            Logger.error(e.getMessage());
        }

    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            Logger.error(e.getMessage());
        }

    }

    /* this method is for debugging purposes; the testharness's output doesn't
     * end up in the main log so we write directly to a local file instead */
    public static void logDebug(String tag, String text) {
        try {
            java.util.List<String> lines = new java.util.ArrayList<>();

            lines.add(text);

            java.nio.file.Path file = java.nio.file.Paths.get("/home/sbr/gin/DEBUG-"+tag+"."+System.nanoTime()+".txt");
            java.nio.file.Files.write(file, lines, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {}
    }


    private String runTest(String command) throws ParseException {

        String testName;
        int rep;
        long timeoutMS;
        String wdField;

        String[] params = command.split(",");
        try {
            if (params.length == 4) {
                testName = params[0];
                rep = Integer.parseInt(params[1]);
                timeoutMS = Long.parseLong(params[2]);
                wdField = params[3];
            } else {
                throw new ParseException("Not a test format: " + command, 0);
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Not a test format: " + command, 0);
        }

        File workingDir = null;
        try {
            if (wdField != null && !wdField.isBlank()) {
                File f = new File(wdField);
                workingDir = f.isAbsolute()
                        ? f.getCanonicalFile()
                        : new File(new File(System.getProperty("user.dir")).getCanonicalFile(), wdField).getCanonicalFile();
            }
        } catch (IOException ignore) {
            workingDir = null;
        }

        // If the caller passed a module dir and it exists, adopt it for file-based tests
        if (workingDir != null && workingDir.isDirectory()) {
            System.setProperty("user.dir", workingDir.getAbsolutePath());
            System.setProperty("basedir",  workingDir.getAbsolutePath());
            System.setProperty("maven.multiModuleProjectDirectory", workingDir.getAbsolutePath());
        }

        UnitTest test = UnitTest.fromString(testName);
        test.setTimeoutMS(timeoutMS);
        //switchToModuleDir(absDir);
        UnitTestResult result = runTest(test, rep);

        return result.toString();

    }

    private UnitTestResult runTest(UnitTest test, int rep) {

        UnitTestResult result = new UnitTestResult(test, rep);

        final String className  = test.getFullClassName();
        final String methodName = normalizeMethodName(test.getMethodName());

        try {
            Class<?> clazz = Class.forName(className);
            boolean jupiterish = looksLikeJupiter(clazz);

            // 1) Try method+class discovery with the right engine bias
            LauncherDiscoveryRequest baseReq = buildRequest(test, jupiterish);

            try (LauncherSession session = LauncherFactory.openSession()) {
                Launcher launcher = session.getLauncher();

                TestPlan plan = launcher.discover(baseReq);
                boolean hasTests = plan.containsTests();

                // 2) If nothing found, try class-only discovery
                if (!hasTests) {
                    var classOnly = LauncherDiscoveryRequestBuilder.request()
                            .selectors(selectClass(clazz))
                            .filters(EngineFilter.includeEngines(
                                    jupiterish ? new String[]{"junit-jupiter"}
                                            : new String[]{"junit-jupiter", "junit-vintage"}))
                            .build();

                    TestPlan plan2 = launcher.discover(classOnly);
                    if (plan2.containsTests()) {
                        plan = plan2;
                        hasTests = true;
                    }
                }

                // 3) If still nothing, consider JUnit 4 fallback only when appropriate
                if (!hasTests && !jupiterish && hasJUnit4Annotations(clazz, methodName)) {
                    Request req = Request.method(clazz, methodName);
                    Result r = new JUnitCore().run(req);

                    UnitTestResult utr = new UnitTestResult(test, rep);
                    utr.setPassed(r.wasSuccessful());
                    for (Failure f : r.getFailures()) {
                        utr.addFailure(new Failure(
                                org.junit.runner.Description.createTestDescription(clazz, methodName),
                                f.getException()));
                    }
                    return utr;
                }

                // 4) If still nothing, report
                if (!hasTests) {
                    result.setPassed(false);
                    result.setExceptionType("gin.test.NoTestsDiscovered");
                    result.setExceptionMessage("No tests discovered for " +
                            className + "#" + methodName + " (Jupiterish=" + jupiterish + ")");
                    return result;
                }

                // 5) Execute the discovered plan (Jupiter will happily run package-private tests)
                launcher.execute(plan, new TestRunListener(result, className, normalizeMethodName(methodName)));

                return result;
            }

        } catch (Throwable t) {
            result.setPassed(false);
            result.setExceptionType(t.getClass().getName());
            result.setExceptionMessage(t.getMessage());
            return result;
        }
    }


    private LauncherDiscoveryRequest buildRequest(UnitTest test, boolean jupiterish)
            throws ClassNotFoundException {

        ClassLoader cl = getClass().getClassLoader();
        Class<?> clazz = cl.loadClass(test.getFullClassName());
        String method = normalizeMethodName(test.getMethodName());

        var builder = LauncherDiscoveryRequestBuilder.request()
                .filters(EngineFilter.includeEngines(
                        jupiterish ? new String[]{"junit-jupiter"}
                                : new String[]{"junit-jupiter", "junit-vintage"}))
                // Jupiter timeout only; harmless for Vintage
                .configurationParameter("junit.jupiter.execution.timeout.test.method.default",
                        test.getTimeoutMS() + " ms");

        // Prefer method-level selector when resolvable; add class as a safety net
        Method m = findMethodDeep(clazz, method);
        if (m != null) {
//            builder.selectors(selectMethod(clazz, m.getName()), selectClass(clazz));
            builder.selectors(selectMethod(clazz, m.getName()));
        } else {
            builder.selectors(selectClass(clazz));
        }

        return builder.build();
    }

    /*
     * Class.getMethod returns only public methods; Class.getDeclaredMethod ignores superclasses
     * This tries to get all methods including superclasses
     */
    public Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            if (clazz.equals(Object.class)) {
                throw e;
            } else {
                return getMethod(clazz.getSuperclass(), methodName);
            }
        }

    }

    // Normalise method name for selectors:
    //  - strip trailing "()"
    //  - strip parameterized suffixes like "[0]", "[arg=…]"
    private static String normalizeMethodName(String m) {
        String s = m;
        if (s.endsWith("()")) s = s.substring(0, s.length() - 2);
        // remove […] suffixes that appear in parameterized display names
        int idx = s.indexOf('[');
        if (idx > 0) s = s.substring(0, idx);
        return s;
    }

    // Walks superclasses to find a declared method (JUnit 3/4 inheritance cases)
    private static java.lang.reflect.Method findMethodDeep(Class<?> c, String name) {
        Class<?> k = c;
        while (k != null && k != Object.class) {
            for (var m : k.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            k = k.getSuperclass();
        }
        return null;
    }

    private static boolean hasJUnit4(Class<?> clazz, String method) {
        try {
            // walk superclasses to find the method
            for (Class<?> k = clazz; k != null && k != Object.class; k = k.getSuperclass()) {
                for (var mm : k.getDeclaredMethods()) {
                    if (mm.getName().equals(method)) {
                        // JUnit 4?
                        if (mm.isAnnotationPresent(org.junit.Test.class)) return true;
                        // JUnit 3 style?
                        boolean isPublic = java.lang.reflect.Modifier.isPublic(mm.getModifiers());
                        boolean isVoid = mm.getReturnType() == Void.TYPE;
                        boolean noArgs = mm.getParameterCount() == 0;
                        if (isPublic && isVoid && noArgs && method.startsWith("test")) return true;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    private static void switchToModuleDir(String abs) {
        if (abs == null || abs.isBlank()) return;
        System.setProperty("user.dir", abs);
        System.setProperty("basedir", abs);
        System.setProperty("maven.multiModuleProjectDirectory", abs);
    }

    private static String cleanForSocket(String s) {
        if (s == null) return "";
        // collapse CR/LF/TAB to single spaces, strip ANSI, then kill commas
        String t = s.replace('\r',' ').replace('\n',' ').replace('\t',' ');
        t = t.replaceAll("\\u001B\\[[;\\d]*m", ""); // strip ANSI color if any
        t = t.replace(',', ';');                    // avoid CSV breakage
        return t.trim();
    }


    private static boolean looksLikeJupiter(Class<?> clazz) {
        try {
            // class-level annotations like @Nested, @TestInstance, @ExtendWith …
            for (var a : clazz.getAnnotations()) {
                if (a.annotationType().getName().startsWith("org.junit.jupiter.")) return true;
            }
            // method-level annotations like @Test, @ParameterizedTest, @RepeatedTest …
            for (var m : clazz.getDeclaredMethods()) {
                for (var a : m.getAnnotations()) {
                    if (a.annotationType().getName().startsWith("org.junit.jupiter.")) return true;
                }
            }
        } catch (Throwable ignore) { }
        return false;
    }

    private static boolean hasJUnit4Annotations(Class<?> clazz, String method) {
        try {
            for (Class<?> k = clazz; k != null && k != Object.class; k = k.getSuperclass()) {
                for (var mm : k.getDeclaredMethods()) {
                    if (!mm.getName().equals(method)) continue;
                    if (mm.isAnnotationPresent(org.junit.Test.class)) return true; // JUnit 4 @Test
                    // JUnit 3 fallback:
                    boolean isPublic = java.lang.reflect.Modifier.isPublic(mm.getModifiers());
                    boolean isVoid   = mm.getReturnType() == Void.TYPE;
                    boolean noArgs   = mm.getParameterCount() == 0;
                    if (isPublic && isVoid && noArgs && method.startsWith("test")) return true;
                }
            }
        } catch (Throwable ignore) { }
        return false;
    }

    private static boolean hasEngine(String id) {
        try {
            java.util.ServiceLoader<org.junit.platform.engine.TestEngine> sl =
                    java.util.ServiceLoader.load(org.junit.platform.engine.TestEngine.class);
            for (var e : sl) {
                if (id.equals(e.getId())) return true;
            }
        } catch (Throwable ignore) {}
        return false;
    }


}
