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

import org.mdkt.compiler.DynamicClassLoader;
import org.mdkt.compiler.InMemoryJavaCompiler;

public class CompilationPOCTwo {

    public static void main(String args[]) throws MalformedURLException, ClassNotFoundException,
            InvocationTargetException, IllegalAccessException {

        if (args.length < 3) {
            System.out.println("Usage: source_filename source_classname test_classname");
            System.exit(0);
        }

        CompilationPOCTwo poc = new CompilationPOCTwo();
        poc.runModifyRerun(args[0], args[1], args[2]);

    }

    private void runModifyRerun(String sourceFilename, String classname, String testClassname) throws
            MalformedURLException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {

        // Parse source file
        CompilationUnit unit = loadAndParseSource(sourceFilename);
        System.out.println(unit.toString());

        // Create our own class loader
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        URL[] urls;
        if (systemClassLoader instanceof URLClassLoader) {
            urls = ((URLClassLoader) systemClassLoader).getURLs();
        } else {
            urls = new URL[]
                    {new File(".").toURI().toURL()};
        }
        ClassLoader parent = systemClassLoader.getParent();
        CacheClassLoader cacheClassLoader = new CacheClassLoader(urls, null);
        cacheClassLoader.addToWhiteList(testClassname); // ok to load this from disk yourself

        // Run the tests, as normal
        runTests(cacheClassLoader, testClassname);

        // Delete the print statement that outputs "Simple One"
        List<Node> nodes = unit.getChildNodes();
        nodes.get(0).getChildNodes().get(1).getChildNodes().get(2).getChildNodes().get(0).remove();

        // Compile the modified class and update in the class loader
        Class newClass = compile("Triangle", unit.toString());
        cacheClassLoader.putInCache("Triangle", newClass);

        // Rerun the tests
        runTests(cacheClassLoader, testClassname);

        System.out.println("Exiting my POC");

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

    private Class compile(String classname, String source) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class<?> after = null;
        try {
            after = InMemoryJavaCompiler.newInstance().compile(classname, source.toString());
        } catch (Exception e) {
            System.err.println("Error compiling in memory: " + e);
            System.exit(-1);
        }

        return after;
    }


    private void runTests(ClassLoader loader, String testClassname) throws ClassNotFoundException {

        Class<?> loadedTestClass = loader.loadClass(testClassname);

        // Instantiate jUnit
        JUnitCore jUnitCore = new JUnitCore();

        System.out.println("Running test class: " + loadedTestClass.getCanonicalName());

        jUnitCore.run(loadedTestClass);

    }

}
