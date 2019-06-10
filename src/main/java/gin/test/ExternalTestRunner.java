package gin.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.pmw.tinylog.Logger;

import gin.Patch;

/**
 * Runs tests externally, by creating a new JVM.
 */
public class ExternalTestRunner extends TestRunner {

    public static final String HARNESS_CLASS = "gin.test.TestHarness";

    private Path temporaryDirectory;
    private Path temporaryPackageDirectory;
    
    private boolean inNewSubprocess;

    /**
     * Create an ExternalTestRunner given a package.ClassName, a classpath string separated by colons if needed,
     * and a list of unit tests that will be used to test patches.
     * @param fullyQualifiedClassName Class name including full package name.
     * @param classPath Standard Java classpath format.
     * @param unitTests List of unit tests to be run against each patch.
     * @param inNewSubprocess Make a new JVM for every test?
     */
    public ExternalTestRunner(String fullyQualifiedClassName, String classPath, List<UnitTest> unitTests, boolean inNewSubprocess) {
        super(fullyQualifiedClassName, classPath, unitTests);
        this.inNewSubprocess = inNewSubprocess;
    }

    public ExternalTestRunner(String fullyQualifiedClassName, String classPath, String testClassName, boolean inNewSubprocess) {
        this(fullyQualifiedClassName, classPath, new LinkedList<UnitTest>(), inNewSubprocess);
        this.setTests(testsForClass(testClassName));
    }

    class TestClient {
        
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
            if (timeoutMS < (long) timeout ) {
                    timeout = (int) timeoutMS;
            }
            clientSocket.setSoTimeout(timeout);
        }

    }

    /**
     * Apply and compile the given patch, then run all unit tests against it.
     * @param patch Patch to apply.
     * @param reps Number of times to run each test.
     * @return the results of the tests
     */
    public UnitTestResultSet runTests(Patch patch, int reps) throws IOException, InterruptedException {

        createTempDirectory();

        // Apply the patch.
        String patchedSource = patch.apply();
        boolean patchValid = (patchedSource != null);

        // Compile in temp dir
        boolean compiledOK = false;
        if (patchValid) {
            compiledOK = compileClassToTempDir(patchedSource);
        }

        // Run tests
        List<UnitTestResult> results;
        if (compiledOK) {
            results = runTests(reps);
        } else {
            results = emptyResults(reps);
        }

        deleteTempDirectory();
        
        return new UnitTestResultSet(patch, patchValid, compiledOK, results);

    }

    /**
     * Create a temporary directory. The patched source file will be written there and compiled.
     * @throws IOException if the directory couldn't be created
     */
    public void createTempDirectory() throws IOException {

        // Create the main temp dir
        this.temporaryDirectory = Files.createTempDirectory(null);
        Logger.info("Created temp dir: " + temporaryDirectory);

        // Create package directory
        String packageName = this.getPackageName();
        String packagePath = packageName.replace(".", "/");
        temporaryPackageDirectory = temporaryDirectory.resolve(packagePath);
        temporaryPackageDirectory.toFile().mkdirs();

    }

    public boolean compileClassToTempDir(String patchedSource) throws FileNotFoundException {

        File sourceFile = temporaryPackageDirectory.resolve(this.getClassNameWithoutPackage() + ".java").toFile();

        // Write to the package dir
        try (PrintWriter out = new PrintWriter(sourceFile)) {
            out.println(patchedSource);
        }

        return Compiler.compileFile(sourceFile, this.getClassPath());

    }

    /**
     * Run each of the tests against the modified class held in the class load, rep times.
     * @param reps Number of times to run each test
     * @return List of Test Results
     */
    private List<UnitTestResult> runTests(int reps) throws IOException, InterruptedException {

        List<UnitTestResult> results = new LinkedList<>();

        File javaHome = new File(System.getProperty("java.home"));
        File javaBin = new File(javaHome, "bin");
        File jvm = new File(javaBin, "java");

        String classpath = this.getTemporaryDirectory() + ":" +
                           this.getClassPath() + ":" +
                           System.getProperty("java.class.path");

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
            new Thread(new Runnable() {
                public void run() {
                    while (scanner.hasNextLine()) {
                        System.out.println(scanner.nextLine());
                    }
                    scanner.close();
                }
            }).start();
            
            // we're spawning a separate process, and if our JVM
            // dies we'll want to kill the other process too,
            // otherwise it'll be left open keeping file and port
            // handles open and causing all kinds of bother.
            // (note - e.g. pressing eclipse red button forcibly
            // kills the JVM so this doesn't fire in that situation;
            // apparently nothing can be done about that)
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                }
            });
                        
            Thread.sleep(1000); // allowing for server startup time

            TestClient client = new TestClient();
            client.startConnection("localhost", port);

            boolean keepConnection = true;

            inner:
            while (index < maxIndex) {
                
                if (keepConnection) {
                        
                        int testIndex = index % this.getTests().size();
                        int rep = index / this.getTests().size();
                        UnitTest test = this.getTests().get(testIndex);
                        Logger.debug("Running test " + index + "/" + maxIndex + ": " + "rep=" + rep+1 + "/"+reps+", " + "testIndex=" + testIndex + "/" + this.getTests().size() + ": " + test);
                        
                        long timeoutMS = test.getTimeoutMS();
                        String testName = test.toString();
                        index++;

                        client.setTimeoutMS(timeoutMS + 500); // extra time for connection overhead

                        String message = testName + "," + String.valueOf(rep+1) + "," + String.valueOf(timeoutMS);
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
                                if (inNewSubprocess) {
                                    keepConnection = false; // process is closed after each test
                                    break inner; // process is closed after each test
                                }
                            } else {
                                // connection timed out
                                keepConnection = false;
                                UnitTestResult result = timeoutResult(test, rep+1);
                                results.add(result);
                                break inner;
                            }
                        } catch (ParseException e) {
                            keepConnection = false;
                            // smth else went wrong, test or test result likely in the wrong format
                            UnitTestResult result = new UnitTestResult(test, rep+1);
                            result.setExceptionType(e.getClass().getName());
                            result.setExceptionMessage(e.getMessage());
                            results.add(result);
                            break;
                        }

                } else {
                    break inner;
                }

            } // end of inner

            try {
                client.sendMessage("stop");
            } catch (SocketTimeoutException e) {}

            client.stopConnection();
                        
            Thread.sleep(500); // cleanup time

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            Thread.sleep(500); // cleanup time

        } // end of outer

        return results;

    }

    private UnitTestResult timeoutResult(UnitTest test, int rep) {

        UnitTestResult result = new UnitTestResult(test, rep);

        long timeoutMS = test.getTimeoutMS();
        String exceptionType = "org.junit.runners.model.TestTimedOutException";
        String exceptionMessage = "test timed out after "+ timeoutMS + "  milliseconds";

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
        if ( (temporaryDirectory != null) && (Files.exists(temporaryDirectory)) && (Files.isDirectory(temporaryDirectory)) ) {
            FileUtils.deleteDirectory(temporaryDirectory.toFile());
            Logger.info("Deleted temp dir: " + temporaryDirectory);
        } else {
            Logger.error("Error deleting " + temporaryDirectory + " - the temporary directory  does not exist.");
        } 
    }

}
