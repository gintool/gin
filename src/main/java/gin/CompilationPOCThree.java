package gin;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Version 1: assume a single top-level package/dir including both source and class files.
 *
 * Need to guard against the directory already being in the classpath.
 *
 */
public class CompilationPOCThree {

    private File workingDirectory;
    private File sourceFile;
    private String className;
    private String testName;
    private GinClassLoader classLoader;

    public static void main(String args[]) {

        if (args.length < 2) {
            System.out.println("Usage: directory classname");
            System.exit(0);
        }

        CompilationPOCThree poc = new CompilationPOCThree(args[0], args[1]);
        poc.runModifyRerun();

    }

    public CompilationPOCThree(String directoryName, String className) {

        this.workingDirectory = new File(directoryName);

        this.className = className;
        this.testName = className + "Test";

        this.sourceFile = new File(this.workingDirectory, className + ".java");

    }

    private void runModifyRerun() {



        // Parse source file
        CompilationUnit unit = loadAndParseSource();
        System.out.println(unit.toString());

        // Run test class through jUnit
        runTests(null);

        // Delete the print statement that outputs "Simple One"
        List<Node> nodes = unit.getChildNodes();
        nodes.get(0).getChildNodes().get(1).getChildNodes().get(2).getChildNodes().get(0).remove();

        // Compile the modified class
        Class<?> modifiedClass = compile(unit.toString());

        // Run jUnit tests
        runTests(modifiedClass);

        System.out.println("Exiting my POC");
    }


    private CompilationUnit loadAndParseSource() {

        // Load and parse source
        CompilationUnit unit = null;

        try {
            unit = JavaParser.parse(sourceFile);
        } catch (IOException io) {
            System.err.println("Exception reading source file: " + sourceFile.getAbsolutePath() + " " + io);
            io.printStackTrace();
            System.exit(-1);
        }

        return unit;

    }

    private void runTests(Class<?> modifiedClass) {

        classLoader = new GinClassLoader(workingDirectory);

        if (modifiedClass != null) {
            classLoader.store(this.className, modifiedClass);
        }

        Class<?> runnerClass = null;

        try {
            runnerClass = classLoader.loadClass(IsolatedTestRunner.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Invoke via reflection (List.class is OK because it just uses the string form of it)
        Object runner = null;
        try {
            runner = runnerClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        Method method = null;
        try {
            method = runner.getClass().getMethod("runTestClasses", List.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        List<String> testClasses = new LinkedList<>();
        testClasses.add(this.testName);

        try {
            method.invoke(runner, testClasses);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Class compile(String source) {

        Class<?> after = null;
        try {
            after = InMemoryJavaCompiler.newInstance().compile(this.className, source);
        } catch (Exception e) {
            System.err.println("Error compiling in memory: " + e);
            System.exit(-1);
        }

        return after;

    }

}
