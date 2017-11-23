package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.mdkt.compiler.InMemoryJavaCompiler;

public class CompilationPOCTwo {

    public static void main(String args[]) throws MalformedURLException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {

        if (args.length < 3) {
            System.out.println("Usage: source_filename source_classname test_classname");
            System.exit(0);
        }

        CompilationPOCTwo poc = new CompilationPOCTwo();
        poc.runModifyRerun(args[0], args[1], args[2]);

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

    private Class compile(String classname, String source) throws InvocationTargetException, IllegalAccessException {
        Class myClass =
        Class<?> helloClass = null;
        try {
            helloClass = InMemoryJavaCompiler.newInstance().compile(classname, source.toString());
        } catch (Exception e) {
            System.err.println("Error compiling in memory: " + e);
            System.exit(-1);
        }
        Method[] methods = helloClass.getMethods();
        methods[0].invoke(this);

        return helloClass;
    }

    private void runModifyRerun(String sourceFilename, String classname, String testClassname) throws MalformedURLException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {

        // Parse source file
        CompilationUnit unit = loadAndParseSource(sourceFilename);
        System.out.println(unit.toString());

        //CacheClassLoader loader = new CacheClassLoader();
        ClassLoader base = ClassLoader.getSystemClassLoader();
        URL[] urls;
        if (base instanceof URLClassLoader) {
            urls = ((URLClassLoader) base).getURLs();
        } else {
            urls = new URL[]
                    {new File(".").toURI().toURL()};
        }
        ClassLoader parent = base.getParent();
        VerboseLoader loader = new VerboseLoader(urls, parent);

        // Run test class through jUnit
        runTests(loader, testClassname);

        // Delete the print statement that outputs "Simple One"
        List<Node> nodes = unit.getChildNodes();
        nodes.get(0).getChildNodes().get(1).getChildNodes().get(2).getChildNodes().get(0).remove();

        System.out.println("After deletion: \n" + unit);

        // Compile the modified class
        Class newClass = compile("Triangle", unit.toString());

        //loader = new CacheClassLoader();
        //loader.setClass(classname, newClass);

        // Run jUnit tests
        runTests(loader, testClassname);

        System.out.println("Exiting my POC");

    }

    private void runTests(ClassLoader loader, String testClassname) throws ClassNotFoundException {

        // Load the Test class. The required class under test will be loaded from the same directory by jUnit.
        Class<?> loadedTestClass = null;
        //loadedTestClass = loader.loadClass(testClassname);
        loadedTestClass = loader.loadClass(testClassname);
        Class<?> loadedCUT = loader.loadClass("Triangle");
        //ClassLoader base = ClassLoader.getSystemClassLoader();
        //loadedTestClass = base.loadClass(testClassname);


        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        System.out.println("Running test class: " + loadedTestClass.getCanonicalName());

        Result result = jUnitCore.run(loadedTestClass);


        System.out.println(result.wasSuccessful());

    }

}
