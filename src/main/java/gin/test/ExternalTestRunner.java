package gin.test;

import gin.Patch;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.SystemOutHandler;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

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

    /**
     * Run each of the tests against the modified class held in the class load, rep times.
     *
     * @param reps Number of times to run each test
     * @return List of Test Results
     */
    private List<UnitTestResult> runTests(int reps) throws IOException, InterruptedException {

        List<UnitTestResult> results = new LinkedList<>();

        File javaHome = new File(System.getProperty("java.home"));
        File javaBin = new File(javaHome, "bin");
        File jvm = new File(javaBin, "java");

        String classpath = this.getTemporaryDirectory() + File.pathSeparator +
                this.getClassPath() + File.pathSeparator +
                System.getProperty("java.class.path");

        classpath = Arrays.stream(classpath.split(File.pathSeparator))
                .map(s -> Paths.get(s).normalize().toFile().getAbsolutePath())
                .collect(Collectors.joining(File.pathSeparator));

        int index = 0;

        int maxIndex = reps * this.getTests().size();

        while (index < maxIndex) {

            // in the following we use sockets to communicate with the
            // TestHarness in a sub process.
            // we don't just capture stdout from the process, because if
            // you're running multiple tests in subprocess you have to
            // communicate somehow to know that a test finished (or not)
            // this doesn't work if the hanging test blocks stdout
            // so: we fire up a subprocess, get it to tell us what port
            // number it wants to use via stdout, then communicate via
            // that port. stdout is redirected to the real System.out
            // so that we can debug the running tests if needed.

            ProcessBuilder builder;
            builder = new ProcessBuilder(jvm.getAbsolutePath(),
                    "-Dtinylog.level=" + Logger.getLevel(),
                    "-cp", classpath,
                    HARNESS_CLASS
            );

            // redirect everything except STDOUT for now as we need it to get the port
            final Process process = builder.redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT).start();
            int port = 0;
            final Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(TestHarness.PORT_PREFIX)) {
                    port = Integer.parseInt(line.substring(line.indexOf("=") + 1));
                    break;
                }
            }

            // having set off the process and grabbed the port number from its stdout, we now
            // redirect its output to the real stdout
            // (no need to kill this thread, it'll exit when the process dies)
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

                int testIndex = index % this.getTests().size();
                int rep = index / this.getTests().size();
                UnitTest test = this.getTests().get(testIndex);
                Logger.debug("Running test " + index + "/" + maxIndex + ": " + "rep=" + rep + 1 + "/" + reps + ", " + "testIndex=" + testIndex + "/" + this.getTests().size() + ": " + test);

                long timeoutMS = test.getTimeoutMS();
                String testName = test.toString();
                index++;

                client.setTimeoutMS(timeoutMS + 500); // extra time for connection overhead

                String message = testName + "," + rep + 1 + "," + timeoutMS;
                String resp;
                try {
                    resp = client.sendMessage(message);
                } catch (SocketTimeoutException e) {
                    resp = null;
                }
                try {
                    if (resp != null) {
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
                                || (eachRepetitionInNewSubProcess && testIndex == this.getTests().size() - 1)
                                // 3) it is fail fast and the test failed
                                || (failFast && !result.getPassed())) {
                            break;
                        }
                    } else {
                        // connection timed out
                        UnitTestResult result = timeoutResult(test, rep + 1);
                        results.add(result);
                        break;
                    }
                } catch (ParseException e) {
                    // smth else went wrong, test or test result likely in the wrong format
                    UnitTestResult result = new UnitTestResult(test, rep + 1);
                    result.setExceptionType(e.getClass().getName());
                    result.setExceptionMessage(e.getMessage());
                    results.add(result);
                    break;
                }

            } // end of inner

            try {
                client.sendMessage("stop");
            } catch (SocketTimeoutException ignored) {
            }

            client.stopConnection();

            Thread.sleep(500); // cleanup time

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            Thread.sleep(500); // cleanup time

            // In case the tests failed, and it is fail fast, then stop the loop
            if (failFast && results.stream()
                    .anyMatch(result -> !result.getPassed())) {
                break;
            }

        } // end of outer

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

}
