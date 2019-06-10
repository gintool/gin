package gin.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.InMemoryJavaCompiler;
import org.pmw.tinylog.Logger;

/**
 * Wraps the InMemoryJavaCompiler to compile a class given its name and a classpath.
 */
public class Compiler {

    /**
     * Compile a class to bytecode, given the fully qualified classname, a source string, and an optional classpath.
     * @param className Full class name, e.g. org.mypackage.StringHelper
     * @param source String of full source file.
     * @param classPath Standard Java classpath string.
     * @return the compiled code
     */
    public static CompiledCode compile(String className, String source, String classPath)  {

        CompiledCode code;

        try {

            InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
            compiler = compiler.ignoreWarnings();

            String fullClassPath = classPath == null ?
                    System.getProperty("java.class.path") :
                    classPath + ":" + System.getProperty("java.class.path");

            compiler.useOptions("-classpath", fullClassPath, "-Xlint:unchecked");

            code = compiler.compileToRawBytes(className, source);

        } catch (Exception e) {

            if (e.getMessage().contains("does not exist")) {
                Logger.error("Did you set the classpath with -cp=?");
            }

            code = null;

        }

        return code;

    }

    public static boolean compileFile(File source, String classPath) {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);

        List<String> options = new ArrayList<>();
        options.add("-cp");
        options.add(System.getProperty("java.class.path") + ":" + classPath);

        Iterable<? extends JavaFileObject> compilationUnit = fm.getJavaFileObjectsFromFiles(Arrays.asList(source));

        JavaCompiler.CompilationTask task;
        task = compiler.getTask(null, fm, null, options, null, compilationUnit);

        if (!task.call()) {
            Logger.warn("Error during compilation of source on disk: " + source);
            return false;
        }

        return true;

    }

}
