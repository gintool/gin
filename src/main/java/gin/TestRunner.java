package gin;

import com.github.javaparser.ast.CompilationUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
    private static final int REPS = 5;

    private Program program;

    public TestRunner(Program program) {
        this.program = program;
    }

    public TestResult test(Patch patch) {

        // Apply the patch
        CompilationUnit compilationUnit = program.getCompilationUnit();
        CompilationUnit patched = compilationUnit.clone();
        patch.apply(patched);

        // Create temp dir
        ensureDirectory(new File(TMP_DIR));

        // Copy patched program and test source to temp directory
        copySource(patched);

        // Compile the patched program and test classes
        boolean compiledOK = compile();

        // Run test cases if compiledOK, otherwise return failure.
        if (compiledOK) {
            return loadClassAndRunTests();
        } else {
            return new TestResult(null, Double.MAX_VALUE, false);
        }

    }

    /**
     * Write the patched source and test class to a temporary directory.
     *
     * @param compilationUnit
     */
    private void copySource(CompilationUnit compilationUnit) {

        // Create temp package subdirectory as per package name
        String packageName = program.getCompilationUnit().getPackageDeclaration().get().getName().toString();
        String packageDirName = packageName.replace(".", File.separator);
        File tmpPackageDir = new File(TMP_DIR, packageDirName);
        tmpPackageDir.mkdirs();

        // Write patched program to temp dir
        String programFilename = new File(program.getFilename()).getName();
        File tmpSourceFile = new File(tmpPackageDir, programFilename);
        try {
            FileWriter writer = new FileWriter(tmpSourceFile);
            writer.write(compilationUnit.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Copy test source to tmp directory
        String originalTestFilename = FilenameUtils.removeExtension(program.getFilename()) + "Test.java";
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
    private boolean compile()  {

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

        String packageName = program.getCompilationUnit().getPackageDeclaration().get().getName().toString();
        String packageDirName = packageName.replace(".", File.separator);
        File tmpPackageDir = new File(TMP_DIR, packageDirName);

        String programFilename = new File(program.getFilename()).getName();
        File sourceFile = new File(tmpPackageDir, programFilename);

        String originalTestFilename = FilenameUtils.removeExtension(program.getFilename()) + "Test.java";
        File originalTestFile = new File(originalTestFilename);
        File testFile = new File(tmpPackageDir, originalTestFile.getName());

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
    private TestResult loadClassAndRunTests() {

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
            String packageName = program.getCompilationUnit().getPackageDeclaration().get().getName().toString();
            String classname = packageName + "." + program.getCompilationUnit().getType(0).getNameAsString();
            loadedTestClass = classLoader.loadClass(classname + "Test");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        // Run the tests REPS times and calculate the mean via a running average
        double meanElapsed = 0;
        Result result = null;
        for (int rep=0; rep < REPS; rep++) {
            try {
                long start = System.nanoTime();
                result = jUnitCore.run(loadedTestClass);
                long elapsed = System.nanoTime() - start;
                meanElapsed += (elapsed - meanElapsed) / (rep+1); // running average
            } catch (Exception e) {
                System.err.println("Error running junit: " + e);
                System.exit(-1);
            }
        }

        return new TestResult(result, meanElapsed, true);

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
     * Helped function to clean a directory.
     * @param f
     */
    private void ensureDirectory(File f) {
        FileUtils.deleteQuietly(f);
        f.mkdirs();
    }

    /**
     * Class to hold the result of running jUnit.
     */
    public class TestResult {
        Result result;
        double averageTime;
        boolean compiled;
        public TestResult(Result result, double averageTime, boolean compiled) {
            this.result = result;
            this.averageTime = averageTime;
            this.compiled = compiled;
        }
    }

}