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

    public static void main(String args[]) {

        if (args.length < 1) {
            System.out.println("Usage: source_filename test_classname");
            System.exit(0);
        }

        CompilationPOC poc = new CompilationPOC();
        poc.runModifyRerun(args[0], args[1]);

    }

    private CompilationUnit loadAndParseSource(String sourceFilename) {

        // Load and parse source
        CompilationUnit unit = null;

        try {
            unit = JavaParser.parse(new File(sourceFilename));
        } catch (IOException io) {
            System.err.println("Exception reading source file: " + sourceFilename + " " + io);
            io.printStackTrace();
            System.exit(-1);
        }

        return unit;

    }

    // Compile a class.
    // TODO: have it write result to memory rather than disk
    private void compile(String classname, String source) {

        // Create an in-memory Java File Object
        JavaFileObject so = null;
        try {
            so = new InMemoryJavaFileObject(classname, source);
        } catch (Exception exception) {
            System.err.println("Error creating memory file object " + exception);
            System.exit(-1);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        // for compilation diagnostic message processing on compilation WARNING/ERROR
        MyDiagnosticListener c = new MyDiagnosticListener();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(c, Locale.ENGLISH,null);
        Iterable options = Arrays.asList("-d", "tmp");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, c, options, null,
                Arrays.asList(so));

        Boolean result = task.call();
        if (result == true) {
            System.out.println("Compilation succeeded");
        }

    }

    private void runModifyRerun(String sourceFilename, String testClassname) {

        // Parse source file
        CompilationUnit unit = loadAndParseSource(sourceFilename);
        System.out.println(unit.toString());

        // Run test class through jUnit
        runTests(testClassname);

        // Delete the print statement that outputs "Simple One"
        List<Node> nodes = unit.getChildNodes();
        nodes.get(0).getChildNodes().get(1).getChildNodes().get(2).getChildNodes().get(0).remove();

        // Compile the modified class
        compile("Triangle", unit.toString());

        // Run jUnit tests
        runTests(testClassname);

        System.out.println("Exiting my POC");

    }

    private void runTests(String testClassname) {

        // Create a class loader initialised for the temp directory.
        //URLClassLoader classLoader = null;
        ClassLoader classLoader = null;

        try {
            //classLoader = new URLClassLoader(new URL[]{new File("/Users/whited/Documents/gin/tmp/").toURI().toURL()});
            classLoader = new Reloader();
        } catch (Exception e) {
            System.err.println("Error instantiating class loader: " + e);
            e.printStackTrace();
        }

        // Load the Test class. The required class under test will be loaded from the same directory by jUnit.
        Class<?> loadedTestClass = null;
        try {
            loadedTestClass = classLoader.loadClass(testClassname);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        System.out.println("Running test class: " + loadedTestClass.getCanonicalName());
        //System.out.println("Running class from: " + loadedTestClass.getProtectionDomain().getCodeSource().getLocation().getPath());

        Result result = jUnitCore.run(loadedTestClass);

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
