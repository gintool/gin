package gin.test;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
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
public class Compiler implements Serializable {

    @Serial
    private static final long serialVersionUID = -5411786808143665676L;

    private String lastError;
    
    public Compiler() {
    	this.lastError = null;
    }
    
    public String getLastError() {
		return lastError;
	}
    
    /**
     * Compile a class to bytecode, given the fully qualified classname, a source string, and an optional classpath.
     *
     * @param className Full class name, e.g. org.mypackage.StringHelper
     * @param source    String of full source file.
     * @param classPath Standard Java classpath string.
     * @return the compiled code
     */
    public CompiledCode compile(String className, String source, String classPath) {

        CompiledCode code;
        lastError = null;

        try {

            InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
            compiler = compiler.ignoreWarnings();

            String fullClassPath = classPath == null ?
                    System.getProperty("java.class.path") :
                    classPath + File.pathSeparator + System.getProperty("java.class.path");

            compiler.useOptions("-classpath", fullClassPath, "-Xlint:unchecked");

            code = compiler.compileToRawBytes(className, source);
            
        } catch (Exception e) {

            if (e.getMessage().contains("does not exist")) {
                Logger.error("Did you set the classpath with -cp=?");
            }

            code = null;
            lastError = e.getMessage();

        }

        return code;

    }

    public boolean compileFile(File source, String classPath) {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<JavaFileObject>();
        boolean compiled = false;
        lastError = null;
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnosticsCollector, null, null)) {
            List<String> options = new ArrayList<>();
            options.add("-cp");
            options.add(classPath + File.pathSeparator + System.getProperty("java.class.path"));

            Iterable<? extends JavaFileObject> compilationUnit = fm.getJavaFileObjectsFromFiles(Collections.singletonList(source));

            JavaCompiler.CompilationTask task;
            task = compiler.getTask(null, fm, diagnosticsCollector, options, null, compilationUnit);

            if (!task.call()) {
                Logger.warn("Error during compilation of source on disk: " + source);
                
                lastError = "";
                List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                    // read error dertails from the diagnostic object
                    lastError += diagnostic.getMessage(null) + System.lineSeparator();
                }
                
            } else {
                compiled = true;
            }
        } catch (IOException ex) {
            Logger.error(ex, "Error while trying to close the StandardJavaFileManager instance.");
        }
        return compiled;
    }

}
