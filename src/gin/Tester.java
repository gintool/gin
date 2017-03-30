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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

public class Tester {

    private static final String TMP_DIR = "tmp" + File.separator;
    private static final int REPS = 1;

    private Program program;
    private String programName;
    private URLClassLoader classLoader = null;

    public Tester(String programName, Program program) {
        this.programName = programName;
        this.program = program;
    }

    public TestResult test(Patch patch) {
        prepareTempDirectory();
        CompilationUnit compilationUnit = program.getCompilationUnit();
        CompilationUnit patched = compilationUnit.clone();
        patch.apply(patched);
        boolean success = compile(patched);
        if (success) {
            return loadClassandRunTests();
        } else {
            System.out.println("Failed to compile");
            return new TestResult(null, Double.MAX_VALUE, false);
        }
    }

    private boolean compile(CompilationUnit compilationUnit)  {

        Path p = Paths.get(programName);
        String sourceFilename = TMP_DIR + File.separator + p.getFileName().toString();
        String testFilename = FilenameUtils.removeExtension(programName) + "Test.java";

        try {
            File sourceFile   = new File(sourceFilename);
            FileWriter writer = new FileWriter(sourceFile);
            writer.write(compilationUnit.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File("tmp")));
        } catch (IOException e) {
            System.err.println("Error configuring compiler: " + e);
        }

        // Compile the file
        LinkedList<File> programFiles = new LinkedList<>();
        programFiles.add(new File (sourceFilename));
        programFiles.add(new File (testFilename));
        CompilerListener diagnosticListener = new CompilerListener();
        boolean success = compiler.getTask(null, fileManager, diagnosticListener, null, null, fileManager.getJavaFileObjectsFromFiles(programFiles)).call();

        try {
            fileManager.close();
        } catch (IOException ioexception) {
            System.err.println("Error closing file manager when compiling: " + ioexception);
        }

        return success;

    }

    private TestResult loadClassandRunTests() {

        try {
            this.classLoader = new URLClassLoader(new URL[]{new File(TMP_DIR).toURI().toURL()});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Class<?> loadedTestClass = null;
        try {
            String classname = program.getCompilationUnit().getType(0).getNameAsString();
            loadedTestClass = classLoader.loadClass(classname + "Test");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        JUnitCore jUnitCore = new JUnitCore();

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

    // Used to silence the compiler
    private static final class CompilerListener implements DiagnosticListener {
        @Override
        public void report(Diagnostic diagnostic) {
        }
    }

    private void prepareTempDirectory() {
        if (Files.exists(Paths.get(TMP_DIR))) {
            try {
                FileUtils.cleanDirectory(new File(TMP_DIR));
            } catch (Exception e) {
                System.err.println("Exception cleaning temporary directory: " + e);
            }
        } else {
            new File(TMP_DIR).mkdirs();
        }
        String testFilename = FilenameUtils.removeExtension(programName) + "Test.java";
        String tmpTestFilename = TMP_DIR + FilenameUtils.getBaseName(testFilename) + ".java";
        try {
            FileUtils.copyFile(new File(testFilename), new File (tmpTestFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

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