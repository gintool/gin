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

public class Tester {

    private static final String TMP_DIR = "tmp" + File.separator;
    private static final int REPS = 5;

    private Program program;
    private URLClassLoader classLoader = null;

    public Tester(Program program) {
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

        boolean success = compile();
        if (success) {
            return loadClassandRunTests();
        } else {
            System.out.println("Failed to compile");
            return new TestResult(null, Double.MAX_VALUE, false);
        }
    }

    private void ensureDirectory(File f) {
        FileUtils.deleteQuietly(f);
        f.mkdirs();
    }

    private void copySource(CompilationUnit compilationUnit) {

        // Create temp package subdirectory as per package name
        String packageName = program.getCompilationUnit().getPackageDeclaration().get().getName().toString();
        String packageDirName = packageName.replace(".", File.separator);
        File tmpPackageDir = new File(TMP_DIR, packageDirName);
        tmpPackageDir.mkdirs();

        // Copy test source to tmp directory
        String programWithoutExtension = FilenameUtils.removeExtension(program.getFilename());
        String origTestFilename = programWithoutExtension + "Test.java";
        File originalTestFile = new File(origTestFilename);

        File tmpTestFile = calcTempTestFile();
        try {
            FileUtils.copyFile(originalTestFile, tmpTestFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write patched program to temp dir
        try {
            FileWriter writer = new FileWriter(calcTempSourceFile());
            writer.write(compilationUnit.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private File calcTempSourceFile() {
        String originalSourceFilename = new File(program.getFilename()).getName();
        File tmpSourceFilename = new File(calcTempPackageDir(), originalSourceFilename);
        return tmpSourceFilename;
    }

    private File calcTempPackageDir() {
        String packageName = program.getCompilationUnit().getPackageDeclaration().get().getName().toString();
        String packageDirName = packageName.replace(".", File.separator);
        return new File(TMP_DIR, packageDirName);
    }

    private File calcTempTestFile() {
        String programBaseName = FilenameUtils.getBaseName(program.getFilename());
        String testFilename = programBaseName + "Test.java";
        File tmpTestFile = new File(calcTempPackageDir(), testFilename);
        return tmpTestFile;
    }

    private boolean compile()  {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        try {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(TMP_DIR)));
        } catch (IOException e) {
            System.err.println("Error configuring compiler: " + e);
        }

        // Compile the file
        LinkedList<File> programFiles = new LinkedList<>();
        File sourceFile = calcTempSourceFile();
        File testFile = calcTempTestFile();
        programFiles.add(sourceFile);
        programFiles.add(testFile);
        CompilerListener diagnosticListener = new CompilerListener();
        boolean success = compiler.getTask(null, fileManager,  diagnosticListener, null, null,
                fileManager.getJavaFileObjectsFromFiles(programFiles)).call();

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