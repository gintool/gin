package gin.test;

import gin.Patch;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Runs tests externally, by creating a new JVM.
 */
public class ExternalTestRunner extends TestRunner {

    public static final String HARNESS_CLASS = "gin.test.TestHarness";
    @Serial
    private static final long serialVersionUID = 2253018385180472967L;
    /**
     * If true, each test is run in a new JVM.
     */
    private final boolean eachTestInNewSubProcess;
    /**
     * If true, each repetition of the full test suite is run in a new JVM.
     */
    private final boolean eachRepetitionInNewSubProcess;
    private Path temporaryDirectory;
    private Path temporaryPackageDirectory;
    /**
     * If set to true, the tests will stop at the first failure and the next
     * patch will be executed. You probably don't want to set this to true for
     * Automatic Program Repair.
     */
    private boolean failFast;

    /**
     * Create an ExternalTestRunner given a package.ClassName, a classpath string separated by colons if needed,
     * and a list of unit tests that will be used to test patches.
     *
     * @param fullyQualifiedClassName       Class name including full package name.
     * @param classPath                     Standard Java classpath format.
     * @param unitTests                     List of unit tests to be run against each patch.
     * @param eachTestInNewSubprocess       Make a new JVM for every test?
     * @param eachRepetitionInNewSubProcess Run each repetition in a new JVM?
     * @param failFast                      option for halting test execution upon first test failure encountered
     */
    public ExternalTestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests, boolean eachRepetitionInNewSubProcess, boolean eachTestInNewSubprocess, boolean failFast) {
        super(fullyQualifiedClassName, classPath, unitTests);
        this.eachTestInNewSubProcess = eachTestInNewSubprocess;
        this.eachRepetitionInNewSubProcess = eachRepetitionInNewSubProcess;
        this.failFast = failFast;
    }

    /**
     * Create an ExternalTestRunner given a package.ClassName, a classpath string separated by colons if needed,
     * and a test class that will be used to test patches.
     *
     * @param fullyQualifiedClassName       Class name including full package name.
     * @param classPath                     Standard Java classpath format.
     * @param testClassName                 Test class used to test the patches.
     * @param eachTestInNewSubprocess       Make a new JVM for every test?
     * @param eachRepetitionInNewSubProcess Run each repetition in a new JVM?
     * @param failFast                      option for halting test execution upon first test failure encountered
     */
    public ExternalTestRunner(String fullyQualifiedClassName, String classPath, String testClassName, boolean eachRepetitionInNewSubProcess, boolean eachTestInNewSubprocess, boolean failFast) {
        this(fullyQualifiedClassName, classPath, new LinkedList<>(), eachRepetitionInNewSubProcess, eachTestInNewSubprocess, failFast);
        this.setTests(testsForClass(testClassName));
    }

    /**
     * Returns whether this runner should fail fast. See {@link #failFast}.
     *
     * @return {@code true} if the runner should stop at the first failed test
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether this runner should fail fast. See {@link #failFast}.
     *
     * @param failFast {@code true} if the runner should stop at the first
     *                 failed test
     */
    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }

    /**
     * Apply and compile the given patch, then run all unit tests against it.
     *
     * @param patch Patch to apply.
     * @param reps  Number of times to run each test.
     * @return the results of the tests
     */
    public UnitTestResultSet runTests(Patch patch, Object metadata, int reps) throws IOException, InterruptedException {

        createTempDirectory();

        // Apply the patch.
        String patchedSource = patch.apply(metadata);
        boolean patchValid = patch.lastApplyWasValid();
        List<Boolean> editsValid = patch.getEditsInvalidOnLastApply();

        // Did the code change as a result of applying the patch?
        boolean noOp = isPatchedSourceSame(patch.getSourceFile().toString(), patchedSource);
        //Initialise with default value
        boolean compiledOK = false;
        List<UnitTestResult> results;
        // Only tries to compile and run when the patch is valid
        // The patch might be invalid due to a couple of edits, which
        // drop to being no-ops; remaining edits might be ok so still
        // try compiling and then running in case of no-op
        Compiler compiler = new Compiler();
        if (patchValid) {
            // Compile
            compiledOK = compileClassToTempDir(patchedSource, compiler);
            // Run tests
            if (compiledOK) {
                results = runTests(reps);
            } else {
                results = emptyResults(reps);
            }
        } else {
            results = emptyResults(reps);
        }

        deleteTempDirectory();

        return new UnitTestResultSet(patch, patchedSource, patchValid, editsValid, compiledOK, compiler.getLastError(), noOp, results);

    }

    /**
     * Create a temporary directory. The patched source file will be written there and compiled.
     *
     * @throws IOException if the directory couldn't be created
     */
    public void createTempDirectory() throws IOException {

        // Create the main temp dir
        this.temporaryDirectory = Files.createTempDirectory(null);
        Logger.info("Created temp dir: " + temporaryDirectory);

        // Create package directory
        String packageName = this.getPackageName();
        String packagePath = packageName.replace(".", File.separator);
        temporaryPackageDirectory = temporaryDirectory.resolve(packagePath);
        temporaryPackageDirectory.toFile().mkdirs();

    }

    public boolean compileClassToTempDir(String patchedSource, Compiler compiler) throws FileNotFoundException {

        File sourceFile = temporaryPackageDirectory.resolve(this.getClassNameWithoutPackage() + ".java").toFile();

        // Write to the package dir
        try (PrintWriter out = new PrintWriter(sourceFile)) {
            out.println(patchedSource);
        }

        return compiler.compileFile(sourceFile, this.getClassPath());

    }

    private List<UnitTestResult> runTests(int reps) throws IOException, InterruptedException {

        List<UnitTestResult> results = new LinkedList<>();

        File javaHome = new File(System.getProperty("java.home"));
        File javaBin  = new File(javaHome, "bin");
        File jvm      = new File(javaBin, "java");

        // Build child classpath: temp dir + project CP (without conflicting JUnit/Vintage) + harness CP
        String childCp = cleanChildClasspath(this.getClassPath());
        String bucket = cpHasJunit6(childCp) ? "junit6" : "junit5";

        // see notes in build.gradle
        // the following if blocks dynamically check for and load junit and related
        // dependencies in the classpath, avoiding clashes with those already
        // bundled with the target project
        // bundled with the target project

        // Ensure launcher exists
        if (!cpHasEntry(childCp, "org/junit/platform/launcher/core/LauncherDiscoveryRequestBuilder.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "launcher.jar"));
        }

        // Ensure platform engine exists
        if (!cpHasEntry(childCp, "org/junit/platform/engine/ConfigurationParameters.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "platform-engine.jar"));
        }

        // Ensure jupiter engine exists
        if (!cpHasEntry(childCp, "org/junit/jupiter/engine/JupiterTestEngine.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "jupiter-engine.jar"));
        }

        // Ensure platform commons exists
        if (!cpHasEntry(childCp, "org/junit/platform/commons/PreconditionViolationException.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "platform-commons.jar"));
        }

        if (!cpHasEntry(childCp, "org/junit/jupiter/api/parallel/ExecutionMode.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "jupiter-api.jar"));
        }

        if (!cpHasEntry(childCp, "org/opentest4j/AssertionFailedError.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "opentest4j.jar"));
        }

        if (!cpHasEntry(childCp, "org/apiguardian/api/API.class")) {
            childCp = appendToClasspath(childCp, extractEmbeddedJar(bucket, "apiguardian.jar"));
        }

        Logger.debug("ETR: bucket=" + bucket + " childCp=" + childCp);
        Logger.debug("ETR injected launcher? " +
                cpHasEntry(childCp, "org/junit/platform/launcher/core/LauncherDiscoveryRequestBuilder.class"));
        Logger.debug("ETR injected platform-engine? " +
                cpHasEntry(childCp, "org/junit/platform/engine/ConfigurationParameters.class"));
        Logger.debug("ETR injected jupiter-engine? " +
                cpHasEntry(childCp, "org/junit/jupiter/engine/JupiterTestEngine.class"));
        Logger.debug("ETR injected platform-commons? " +
                cpHasEntry(childCp, "org/junit/platform/commons/PreconditionViolationException.class"));
        Logger.debug("ETR injected jupiter-api? " +
                cpHasEntry(childCp, "org/junit/jupiter/api/parallel/ExecutionMode.class"));
        Logger.debug("ETR injected opentest4j? " +
                cpHasEntry(childCp, "org/opentest4j/AssertionFailedError.class"));
        Logger.debug("ETR injected apiguardian? " +
                cpHasEntry(childCp, "org/apiguardian/api/API.class"));

        String rawClasspath = this.getTemporaryDirectory() + File.pathSeparator +
                childCp + File.pathSeparator +
                System.getProperty("java.class.path");

        // IMPORTANT: make entries absolute so changing working dir doesn't break resolution
        String classpath = gin.util.JavaUtils.normalizeAndDedupeClasspath(rawClasspath);

        // Group tests by module, preserving original order
        Map<String, List<UnitTest>> testsByModule = this.getTests().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        UnitTest::getModuleName,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));

        // Project root = current working dir of the parent process (biojava top-level)
        Path projectRoot = java.nio.file.Paths.get("").toAbsolutePath();

        for (Map.Entry<String, List<UnitTest>> e : testsByModule.entrySet()) {
            final String moduleName = e.getKey();
            final List<UnitTest> moduleTests = e.getValue();

            File moduleDir = (moduleName == null || moduleName.isEmpty())
                    ? projectRoot.toFile()
                    : projectRoot.resolve(moduleName).toFile();

            if (!moduleDir.isDirectory()) {
                org.pmw.tinylog.Logger.warn("Module dir not found: " + moduleDir +
                        ", falling back to project root " + projectRoot);
                moduleDir = projectRoot.toFile();
            }

            org.pmw.tinylog.Logger.debug("Starting TestHarness for module '" + moduleName +
                    "' in " + moduleDir.getAbsolutePath());

            int index = 0;
            int maxIndex = reps * moduleTests.size();

            // == Start one harness JVM for THIS MODULE ==
            while (index < maxIndex) {

                String moduleClasspath = withModuleOutputsFirst(classpath, moduleDir);

                List<String> cmd = new ArrayList<>();
                cmd.add(jvm.getAbsolutePath());
                cmd.add("-Dtinylog.level=" + Logger.getLevel());

                // add javaagent if present
                findSystemExitAgentJar(classpath).ifPresent(jar -> cmd.add("-javaagent:" + jar));

                cmd.add("-cp");
                cmd.add(moduleClasspath);
                cmd.add(HARNESS_CLASS);

                ProcessBuilder builder = new ProcessBuilder(cmd);
                builder.directory(moduleDir);

                final Process process = builder
                        .redirectError(Redirect.INHERIT)
                        .redirectInput(Redirect.INHERIT)
                        .start();

                int port = 0;
                final Scanner scanner = new Scanner(process.getInputStream());
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith(TestHarness.PORT_PREFIX)) {
                        port = Integer.parseInt(line.substring(line.indexOf('=') + 1));
                        break;
                    }
                }

                // Stream the rest of the child stdout (debug / test output)
                new Thread(() -> {
                    while (scanner.hasNextLine()) {
                        System.out.println(scanner.nextLine());
                    }
                    scanner.close();
                }).start();

            // we're spawning a separate process, and if our JVM
            // dies we'll want to kill the other process too,
            // otherwise it'll be left open keeping file and port
            // handles open and causing all kinds of bother.
            // (note - e.g. pressing eclipse red button forcibly
            // kills the JVM so this doesn't fire in that situation;
            // apparently nothing can be done about that)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }));

            Thread.sleep(1000); // allowing for server startup time

                TestClient client = new TestClient();
                client.startConnection("localhost", port);

                while (index < maxIndex) {
                    int testIndex = index % moduleTests.size();
                    int rep       = index / moduleTests.size();
                    UnitTest test = moduleTests.get(testIndex);

                    org.pmw.tinylog.Logger.debug("Running test " + index + "/" + maxIndex +
                            " in module '" + moduleName + "': rep=" + (rep + 1) + "/" + reps +
                            ", testIndex=" + testIndex + "/" + moduleTests.size() +
                            ": " + test);


                    long timeoutMS = test.getTimeoutMS();
                    String testName = test.toString();
                    index++;

                    client.setTimeoutMS(timeoutMS + 500);

                    String message = testName + "," + (rep + 1) + "," + timeoutMS + "," + moduleDir.getAbsolutePath();

                    String resp = null;
                    boolean timedOut = false;

                    try {
                        resp = client.sendMessage(message);   // returns a line, or null if peer closed
                    } catch (java.net.SocketTimeoutException ste) {
                        timedOut = true;                      // socket timeout
                        resp = null;
                    }

                    try {
                        if (resp != null) {
                            if (!resp.contains("crash")) {

                                UnitTestResult result = UnitTestResult.fromString(resp, timeoutMS);
                                results.add(result);

                                // closes the connection and creates a new sub-
                                // process if:
                                // 1) new subprocess for each test
                                if (eachTestInNewSubProcess
                                        // 2) it is the last test of the
                                        // repetition. This is needed to avoid
                                        // test poisoning from one repetition to
                                        // another
                                        || (eachRepetitionInNewSubProcess && testIndex == moduleTests.size() - 1)
                                        // 3) it is fail fast and the test failed
                                        || (failFast && !result.getPassed())) {
                                    break;
                                }
                            } else {
                                Logger.error("TestHarness crashed... " + resp);
                            }

                        } else if (timedOut) {
                            // The harness didn't respond within the socket SO_TIMEOUT
                            UnitTestResult result = timeoutResult(test, rep + 1);
                            results.add(result);
                            break;

                        } else {
                            // The harness closed the connection without sending a result line
                            UnitTestResult result = new UnitTestResult(test, rep + 1);
                            result.setPassed(false);
                            result.setTimedOut(false); // distinguish from real timeout
                            result.setExceptionType("gin.test.ExternalTestRunner$ChildTerminated");
                            result.setExceptionMessage("Harness closed connection without sending a result");
                            // optionally: record some minimal timing info
                            results.add(result);
                            break;
                        }

                    } catch (java.text.ParseException pe) {
                        // smth else went wrong, test or test result likely in the wrong format
                        UnitTestResult result = new UnitTestResult(test, rep + 1);
                        result.setPassed(false);
                        result.setExceptionType(pe.getClass().getName());
                        result.setExceptionMessage(pe.getMessage());
                        results.add(result);
                        break;
                    }

                }

                try {
                    client.sendMessage("stop");
                } catch (java.net.SocketTimeoutException ignored) { }
                client.stopConnection();

                Thread.sleep(500); // cleanup time

                if (process.isAlive()) process.destroyForcibly();
                Thread.sleep(500); // cleanup time

                // In case the tests failed, and it is fail fast, then stop the loop
                if (failFast && results.stream().anyMatch(r -> !r.getPassed())) {
                    index = maxIndex; // break module loop
                }
            } // while index<maxIndex (per module)
        } // for each module

        return results;
    }


    private UnitTestResult timeoutResult(UnitTest test, int rep) {

        UnitTestResult result = new UnitTestResult(test, rep);

        long timeoutMS = test.getTimeoutMS();
        String exceptionType = "org.junit.runners.model.TestTimedOutException";
        String exceptionMessage = "test timed out after " + timeoutMS + "  milliseconds";

        result.setPassed(false);
        result.setTimedOut(true);
        result.setExceptionType(exceptionType);
        result.setExceptionMessage(exceptionMessage);
        result.setExecutionTime(timeoutMS * 1000000L);
        result.setCPUTime(timeoutMS * 1000000L);

        return result;
    }

    public Path getTemporaryDirectory() {
        return temporaryDirectory;
    }

    public Path getTemporaryPackageDirectory() {
        return temporaryPackageDirectory;
    }

    public void deleteTempDirectory() throws IOException {
        if ((temporaryDirectory != null) && (Files.exists(temporaryDirectory)) && (Files.isDirectory(temporaryDirectory))) {
            FileUtils.deleteDirectory(temporaryDirectory.toFile());
            Logger.info("Deleted temp dir: " + temporaryDirectory);
        } else {
            Logger.error("Error deleting " + temporaryDirectory + " - the temporary directory  does not exist.");
        }
    }

    static class TestClient {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public void startConnection(String ip, int port) throws IOException {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        public String sendMessage(String msg) throws IOException {
            out.println(msg);
            return in.readLine();
        }

        public void stopConnection() throws IOException {
            in.close();
            out.close();
            clientSocket.close();
        }

        public void setTimeoutMS(long timeoutMS) throws SocketException { // consider changing type of timeoutMS in UnitTest to int
            int timeout = Integer.MAX_VALUE;
            if (timeoutMS < (long) timeout) {
                timeout = (int) timeoutMS;
            }
            clientSocket.setSoTimeout(timeout);
        }

    }

    // Remove any JUnit libs from a *child* classpath string (File.pathSeparator-joined)
    // Projects using other JUnit versions caused clashes (NoSuchMethodError)
    // so we strip those and use the JUnit libs that ship with Gin itself
    private static String cleanChildClasspath(String childClasspath) {
        if (childClasspath == null || childClasspath.isBlank()) return childClasspath;

        // keep order, drop dups
        java.util.LinkedHashSet<String> kept = new java.util.LinkedHashSet<>();
        for (String raw : childClasspath.split(java.io.File.pathSeparator)) {
            if (raw == null || raw.isBlank()) continue;
            String norm = java.nio.file.Paths.get(raw.trim()).normalize().toString();
            // If it doesn't exist, keep it anyway (could be a directory created later)
            //if (!isJUnitLibPath(norm)) {
            if (!isJUnit4OrVintageLibPath(norm)) {
                kept.add(norm);
            }
        }
        return String.join(java.io.File.pathSeparator, kept);
    }

    /** True if path looks like a JUnit Platform/Jupiter/Vintage/OpenTest4J/API Guardian/JUnit4 jar. */
    private static boolean isJUnitLibPath(String path) {
        String name = new java.io.File(path).getName().toLowerCase(java.util.Locale.ROOT);
        // jars (common cases)
        if (name.startsWith("junit-platform-")) return true;       // launcher, engine, commons, reporting
        if (name.startsWith("junit-jupiter-")) return true;        // api, engine, params
        if (name.startsWith("junit-vintage-")) return true;
        if (name.startsWith("opentest4j-")) return true;
        if (name.startsWith("apiguardian-api-")) return true;
        if (name.matches("^junit-\\d+.*\\.jar$")) return true;     // junit-4.x.jar, junit-5 aggregator jars

        // directories on classpath that clearly point to those groupIds (rare but safe)
        String p = path.replace('\\', '/'); // OS-agnostic
        if (p.contains("/org/junit/platform/")) return true;
        if (p.contains("/org/junit/jupiter/")) return true;
        if (p.contains("/org/junit/vintage/")) return true;
        if (p.contains("/org/opentest4j/")) return true;
        if (p.contains("/org/apiguardian/")) return true;

        return false;
    }

    /** True if path looks like a JUnit *4* or Vintage jar that we want to exclude.
     *  We KEEP Jupiter + Platform + opentest4j + apiguardian.
     */
    private static boolean isJUnit4OrVintageLibPath(String path) {
        String name = new java.io.File(path).getName().toLowerCase(java.util.Locale.ROOT);
        // Exclude JUnit 4 and the Vintage engine
        if (name.startsWith("junit-vintage-")) return true;          // Vintage engine
        //if (name.matches("^junit-\\d+.*\\.jar$")) return true;       // junit-4.x.jar - later decided to comment this out. the library is needed for legacy builds!

        // Also exclude obvious Vintage directories on classpath
        String p = path.replace('\\', '/');
        if (p.contains("/org/junit/vintage/")) return true;

        // DO NOT exclude Jupiter or Platform or their friends.
        return false;
    }

    private static boolean cpHasJunit6(String cp) {
        if (cp == null || cp.isBlank()) return false;

        for (String p : cp.split(File.pathSeparator)) {
            String name = new File(p).getName().toLowerCase(Locale.ROOT);

            // Strong signal: Jupiter 6.x present
            if (name.startsWith("junit-jupiter-") && name.contains("-6.")) return true;
            if (name.startsWith("junit-jupiter-engine-") && name.contains("-6.")) return true;
            if (name.startsWith("junit-jupiter-api-") && name.contains("-6.")) return true;

            // Optional: also treat JUnit Platform Suite 6.x as “JUnit 6-era”
            if (name.startsWith("junit-platform-suite-") && name.contains("-6.")) return true;
        }
        return false;
    }

    private static Optional<String> findSystemExitAgentJar(String cp) {
        if (cp == null) return Optional.empty();
        for (String e : cp.split(File.pathSeparator)) {
            if (e == null) continue;
            String n = new File(e).getName();
            if (n.startsWith("junit5-system-exit-") && n.endsWith(".jar")) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    private static String appendIfDirExists(String cp, File dir) {
        if (dir != null && dir.isDirectory()) {
            String p = dir.getAbsolutePath();
            if (!cp.contains(p)) {
                return cp + File.pathSeparator + p;
            }
        }
        return cp;
    }

    private static String withModuleOutputsFirst(String baseCp, File moduleDir) {
        String cp = baseCp;

        File testClasses = new File(moduleDir, "target/test-classes");
        File mainClasses = new File(moduleDir, "target/classes");

        // Prepend (order matters: test-classes before classes)
        List<String> parts = new ArrayList<>();
        if (testClasses.isDirectory()) parts.add(testClasses.getAbsolutePath());
        if (mainClasses.isDirectory()) parts.add(mainClasses.getAbsolutePath());

        // Keep the existing cp after
        parts.add(cp);

        return String.join(File.pathSeparator, parts);
    }


    private static Optional<Path> extractEmbeddedJar(String bucket, String fileName) {
        String resPath = "/embedded-libs/" + bucket + "/" + fileName;
        try (InputStream in = ExternalTestRunner.class.getResourceAsStream(resPath)) {
            if (in == null) return Optional.empty();

            Path tmpDir = Files.createTempDirectory("gin-junit-runtime-");
            tmpDir.toFile().deleteOnExit();

            Path out = tmpDir.resolve(fileName);
            try (OutputStream os = Files.newOutputStream(out)) {
                in.transferTo(os);
            }

            out.toFile().deleteOnExit();
            return Optional.of(out);
        } catch (IOException e) {
            Logger.error("Failed to extract embedded jar " + resPath + ": " + e);
            return Optional.empty();
        }
    }

    private static String appendToClasspath(String cp, Optional<Path> jar) {
        if (jar.isEmpty()) return cp;
        return cp + File.pathSeparator + jar.get().toAbsolutePath();
    }

    private static boolean cpHasEntry(String cp, String entry) {
        if (cp == null || cp.isBlank()) return false;

        for (String p : cp.split(File.pathSeparator)) {
            if (p == null || p.isBlank()) continue;

            File f = new File(p);
            if (!f.exists()) continue;

            try {
                if (f.isDirectory()) {
                    if (new File(f, entry).isFile()) return true;
                } else if (f.isFile() && f.getName().endsWith(".jar")) {
                    try (java.util.jar.JarFile jf = new java.util.jar.JarFile(f)) {
                        if (jf.getEntry(entry) != null) return true;
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return false;
    }

    private static String buildHarnessClasspath() {
        String ginJar = System.getProperty("gin.jar");

        if (ginJar != null && !ginJar.isBlank()) {
            return new File(ginJar).getAbsolutePath();
        }

        // Fallback only if gin.jar is somehow unavailable
        String parentCp = System.getProperty("java.class.path", "");
        LinkedHashSet<String> kept = new LinkedHashSet<>();

        for (String raw : parentCp.split(File.pathSeparator)) {
            if (raw == null || raw.isBlank()) continue;
            String norm = new File(raw).getAbsolutePath();
            if (!isLoggingLibPath(norm)) {
                kept.add(norm);
            }
        }

        return String.join(File.pathSeparator, kept);
    }

    private static boolean isLoggingLibPath(String path) {
        String name = new File(path).getName().toLowerCase(Locale.ROOT);
        String p = path.replace('\\', '/').toLowerCase(Locale.ROOT);

        if (name.startsWith("slf4j-simple-")) return true;
        if (name.startsWith("slf4j-reload4j-")) return true;
        if (name.startsWith("logback-classic-")) return true;
        if (name.startsWith("logback-core-")) return true;
        if (name.startsWith("log4j-to-slf4j-")) return true;
        if (name.startsWith("log4j-slf4j-impl-")) return true;
        if (name.startsWith("log4j-slf4j2-impl-")) return true;

        if (name.equals("slf4j-simple.jar")) return true;
        if (name.equals("slf4j-reload4j.jar")) return true;
        if (name.equals("logback-classic.jar")) return true;
        if (name.equals("logback-core.jar")) return true;

        if (p.contains("/ch/qos/logback/")) return true;
        if (p.contains("/org/slf4j/impl/")) return true;
        if (p.contains("/org/apache/logging/slf4j/")) return true;

        return false;
    }

}
