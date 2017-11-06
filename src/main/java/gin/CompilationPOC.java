package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.apache.commons.io.FilenameUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

    // This should eventually:
    // 1. Run test.
    // 2. Modify source.
    // 3. Compile in memory.
    // 4. Re-run test
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

        // Work out name of the test class given the original source filename as input from commandline
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

        // Run jUnit tests
        runTests(1, testClass);

        // Now alter and dynamically compile and load and run again
        List<Node> nodes = unit.getChildNodes();

        // Delete the print statement that outputs "Simple One"
        nodes.get(0).getChildNodes().get(1).getChildNodes().get(2).getChildNodes().get(0).remove();

        // Now dynamically compile
        // See http://www.beyondlinux.com/2011/07/20/3-steps-to-dynamically-compile-instantiate-and-run-a-java-class/

        // Create an in-memory Java File Object
        JavaFileObject so = null;
        try {
            so = new InMemoryJavaFileObject("Triangle", unit.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        //get system compiler:
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // I think we can provide a new filemanager that will provide an in-memory Java File Object when requesting a file,
        // so we intercept the writing of the Java class to disk.

        // BUT - for now, writing to disk is enough IFF we can load the file from disk and override the already loaded one

        // for compilation diagnostic message processing on compilation WARNING/ERROR
        MyDiagnosticListener c = new MyDiagnosticListener();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(c,
                Locale.ENGLISH,
                null);
        //specify classes output folder
        Iterable options = Arrays.asList("-d", "tmp");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                c, options, null,
                Arrays.asList(so));
        Boolean result = task.call();
        if (result == true)
        {
            System.out.println("Succeeded");
        }

        // NOW - Try to run the test again!

        // Create a class loader initialised for the temp directory.
        URLClassLoader classLoader = null;

        try {
            classLoader = new URLClassLoader(new URL[]{new File("tmp/").toURI().toURL()});
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Load the Test class. The required class under test will be loaded from the same directory by jUnit.
        Class<?> loadedClass = null;
        try {
            loadedClass = classLoader.loadClass(sourceFilenameWithoutExtension);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        Result result2 = jUnitCore.run(loadedClass);

        System.out.println(result2.wasSuccessful());

        System.out.println("Exiting my POC");

        // NEXT STEPS - to (1) not have to write the class to file; (2) Get it loading

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

    public static class InMemoryJavaFileObject extends SimpleJavaFileObject
    {
        private String contents = null;

        public InMemoryJavaFileObject(String className, String contents) throws Exception
        {
            super(URI.create("string:///" + className.replace('.', '/')
                    + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.contents = contents;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException
        {
            return contents;
        }
    }

    public static class MyDiagnosticListener implements DiagnosticListener<JavaFileObject>
    {
        public void report(Diagnostic<? extends JavaFileObject> diagnostic)
        {

            System.out.println("Line Number->" + diagnostic.getLineNumber());
            System.out.println("code->" + diagnostic.getCode());
            System.out.println("Message->"
                    + diagnostic.getMessage(Locale.ENGLISH));
            System.out.println("Source->" + diagnostic.getSource());
            System.out.println(" ");
        }
    }

}
