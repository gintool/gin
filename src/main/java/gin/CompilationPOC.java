package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CompilationPOC {

    protected SourceFile sourceFile;
    protected TestRunner testRunner;

    public static void main(String args[]) {

        if (args.length < 1) {
            System.out.println("Please provide filename of source file to optimise.");
            System.exit(0);
        }

        CompilationPOC poc = new CompilationPOC();
        poc.go(args[0]);

    }

    private void go(String sourceFilenamePath) {

        sourceFile = new SourceFile(sourceFilenamePath);  // just parses the code and counts statements etc.
        CompilationUnit unit = null;

        try {
            unit = JavaParser.parse(new File(sourceFilenamePath));
        } catch (IOException io) {
            System.err.println("Exception reading source file: " + sourceFilenamePath);
            System.err.println("Exception: " + io);
            io.printStackTrace();
            System.exit(-1);
        }

        System.out.println(unit.toString());

        File sourceFile = new File(sourceFilenamePath);
        String sourceFilename = sourceFile.getName();
        String sourceFilenameWithoutExtension = FilenameUtils.removeExtension(sourceFilename);
        String testClassName = sourceFilenameWithoutExtension + "Test";
        Class testClass = null;
        try {
            testClass = Class.forName(testClassName);
        } catch (ClassNotFoundException notFound) {
            System.err.println("Cannot find test class: " + testClassName);
            System.err.println(notFound);
            notFound.printStackTrace();
            System.exit(0);
        }
        runTests(1, testClass);

        // Now alter and dynamically compile and load and run again


        //Patch patch = new Patch(sourceFile);
        //testRunner.test(patch);

    }

    private void runTests(int reps, Class testClass) {

        // Create a class loader initialised for the temp directory.
//        URLClassLoader classLoader = null;
//
//        try {
//            classLoader = new URLClassLoader(new URL[]{new File(TMP_DIR).toURI().toURL()});
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }

        // Load the Test class. The required class under test will be loaded from the same directory by jUnit.
//        Class<?> loadedTestClass = null;
//        try {
//            String classname = FilenameUtils.removeExtension(new File(sourceFile.getFilename()).getName());
//            loadedTestClass = classLoader.loadClass(classname + "Test");
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        Result result = jUnitCore.run(testClass);

        System.out.println(result.wasSuccessful());

    }
}
