package gin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;

public class TestRunner {

    private static final String TMP_DIR = "tmp" + File.separator;
    private static final int DEFAULT_REPS = 1;

    protected SourceFile sourceFile;

    public TestRunner(SourceFile classSource) {
        this.sourceFile = classSource;
    }

    public TestResult test(Patch patch) {
        return test(patch, DEFAULT_REPS);
    }

    public File getTmpDir() {
        return new File (TMP_DIR);
    }

    public TestResult test(Patch patch, int reps) {

        // Apply the patch
        SourceFile patchedSource = patch.apply();

        // If unable to apply patch, report as invalid
        if (patchedSource == null) {
            return new TestResult(null, -1, false, false, "");
        }

        // Create temp dir
        ensureDirectory(new File(TMP_DIR));

        // Copy patched sourceFile and test source to temp directory
        copySource(patchedSource);

        // Compile the patched sourceFile and test classes
        boolean compiledOK = compile();

        // If failed to compile, return with partial result
        if (!compiledOK) {
            return new TestResult(null, -1, false, true, patchedSource.getSource());
        }

        // Otherwise, run tests and return
        TestResult result = loadClassAndRunTests(reps);
        result.patchedProgram = patchedSource.getSource();
        return result;

    }

    /**
     * Write the patched source and test class to a temporary directory.
     *
     * @param patchedProgram The original sourceFile with a patch applied, to be written to the temp directory.
     */
    protected void copySource(SourceFile patchedProgram) {

        File tmpPackageDir = new File(TMP_DIR);
        tmpPackageDir.mkdirs();

        // Write patched sourceFile to temp dir
        String programFilename = new File(sourceFile.getFilename()).getName();
        File tmpSourceFile = new File(tmpPackageDir, programFilename);
        try {
            FileWriter writer = new FileWriter(tmpSourceFile);
            writer.write(patchedProgram.getSource());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Copy test source to tmp directory
        String originalTestFilename = FilenameUtils.removeExtension(sourceFile.getFilename()) + "Test.java";
        File originalTestFile = new File(originalTestFilename);

        File tmpTestFile = new File(tmpPackageDir, originalTestFile.getName());

        try {
            FileUtils.copyFile(originalTestFile, tmpTestFile);
        } catch (IOException e) {
            System.err.println("Error copying test class to temporary directory.");
            System.err.println(originalTestFile + " -> " + tmpTestFile);
            e.printStackTrace();
            System.exit(-1);
        }

    }

    /**
     * Compile the temporary (patched) source file and a copy of the test class.
     *
     * @return Boolean indicating whether the compilation was successful.
     */
    protected boolean compile()  {

        // Configure the compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(TMP_DIR)));
        } catch (IOException e) {
            System.err.println("Error configuring compiler: " + e);
        }

        // Add the source files to a list
        LinkedList<File> programFiles = new LinkedList<>();

        File tmpDir = new File(TMP_DIR);

        String programFilename = new File(sourceFile.getFilename()).getName();
        File sourceFile = new File(tmpDir, programFilename);

        String originalTestFilename = FilenameUtils.removeExtension(this.sourceFile.getFilename()) + "Test.java";
        File originalTestFile = new File(originalTestFilename);
        File testFile = new File(tmpDir, originalTestFile.getName());

        programFiles.add(sourceFile);
        programFiles.add(testFile);

        // Compile the files
        CompilerListener diagnosticListener = new CompilerListener();
        boolean success = compiler.getTask(null, fileManager,  diagnosticListener, null, null,
                fileManager.getJavaFileObjectsFromFiles(programFiles)).call();

        try {
            fileManager.close();
        } catch (IOException ioException) {
            System.err.println("Error closing file manager when compiling: " + ioException);
            ioException.printStackTrace();
            System.exit(-1);
        }

        return success;

    }

    /**
     * Run the tests against the patched class.
     *
     * @return TestResult giving the outcome of running jUnit.
     */
    private TestResult loadClassAndRunTests(int reps) {

        // Create a class loader initialised for the temp directory.
        URLClassLoader classLoader = null;

        try {
            classLoader = new URLClassLoader(new URL[]{new File(TMP_DIR).toURI().toURL()});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Load the Test class. The required class under test will be loaded from the same directory by jUnit.
        Class<?> loadedTestClass = null;
        try {
            String classname = FilenameUtils.removeExtension(new File(sourceFile.getFilename()).getName());
            loadedTestClass = classLoader.loadClass(classname + "Test");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        // Run the tests REPS times and calculate the mean via a running average
        double[] elapsed = new double[reps];
        Result result = null;
        for (int rep=0; rep < reps; rep++) {
            try {
                long start = System.nanoTime();
                result = jUnitCore.run(loadedTestClass);
                elapsed[rep] = System.nanoTime() - start;
            } catch (Exception e) {
                System.err.println("Error running junit: " + e);
                System.exit(-1);
            }
        }

        double thirdQuartile = new DescriptiveStatistics(elapsed).getPercentile(75);
        return new TestResult(result, thirdQuartile, true, true, "");

    }

    /**
     * Callback for the compilation. Used to silence the compiler.
      */
    private static final class CompilerListener implements DiagnosticListener {
        @Override
        public void report(Diagnostic diagnostic) {
        }
    }

    /**
     * Helper function to clean a directory.
     * @param f
     */
    protected void ensureDirectory(File f) {
        FileUtils.deleteQuietly(f);
        f.mkdirs();
    }

    /**
     * Class to hold the junitResult of running jUnit.
     */
    public class TestResult {
        String patchedProgram = "";
        Result junitResult = null;
        double executionTime = -1;
        boolean compiled = false;
        boolean patchSuccess = false;
        public TestResult(Result result, double executionTime, boolean compiled, boolean patchedOK,
                          String patchedProgram) {
            this.junitResult = result;
            this.executionTime = executionTime;
            this.compiled = compiled;
            this.patchSuccess = patchedOK;
            this.patchedProgram = patchedProgram;
        }
        public String toString() {
            boolean junitOK = false;
            if (this.junitResult != null) {
                junitOK = this.junitResult.wasSuccessful();;
            }
            return String.format("Patch Valid: %b; Compiled: %b; Time: %f; Passed: %b", this.patchSuccess,
                    this.compiled, this.executionTime, junitOK);
        }
    }

}