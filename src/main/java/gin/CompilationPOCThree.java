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
    private CacheClassLoader classLoader;



    private void runTests(Class<?> modifiedClass) {

        classLoader = new CacheClassLoader(workingDirectory);

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



}
