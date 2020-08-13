package gin.test.classloader;

import gin.test.JUnitBridge;
import org.apache.commons.lang3.ArrayUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mdkt.compiler.InMemoryJavaCompiler;

/**
 * Intercepts classloading of {@link JUnitBridge}, and provides access to class
 * from the target system. Also allows overlaying of the modified classes.
 */
public class CacheClassLoader extends URLClassLoader {

    /**
     * {@link JUnitBridge} class name.
     */
    private static final String BRIDGE_CLASS_NAME = gin.test.JUnitBridge.class.getName();

    /**
     * Set of custom compiled code to be injected during class loading.
     */
    protected Map<String, byte[]> customCompiledCode = new HashMap<>();

    /**
     * Set of ClassPath elements provided by the user.
     */
    private URL[] providedClassPath;

    /**
     * Constructs a {@code ClassLoader} with the system classpath and the
     * elements given as input.
     *
     * @param classPaths classpath elements to append to the system's classpath
     */
    public CacheClassLoader(URL[] classPaths) {
        super(addSystemClassPath(classPaths), null);
        providedClassPath = classPaths;
    }

    /**
     * Constructs a {@code ClassLoader} with the system classpath and the
     * elements given as input. The input should be a full classpath with
     * elements separated by a colon.
     *
     * @param classpath a : separated classpath to append to the system's
     *                  classpath
     */
    public CacheClassLoader(String classpath) {
        this(classPathToURLs(classpath));
    }

    /**
     * If the class can't be found using parents (I don't have any) then drops
     * back here.
     *
     * @throws java.lang.ClassNotFoundException
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // I have to intervene here, to ensure JUnitBridge uses me in the future.
        if (name.equals(BRIDGE_CLASS_NAME)) {
            return this.findBridgeClass(name);
        }
        // Modified class? Return the modified code.
        if (customCompiledCode.containsKey(name)) {
            return this.findCustomCompiledCode(name);
        }
        // Otherwise, try the system class loader.
        try {
            return this.findSystemClass(name);
        } catch (ClassNotFoundException ex) {
            // If not there, must be part of the project, so load ourselves.
            return this.findProjectClass(name);
        }
    }

    /**
     * Loads the {@link JUnitBridge} class.
     *
     * @param name {@link JUnitBridge} class name. See
     *             {@link #BRIDGE_CLASS_NAME}
     * @return the class representing the JUnit's Bridge
     * @throws ClassNotFoundException
     */
    protected Class<?> findBridgeClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    /**
     * Loads custom compiled classes.
     *
     * @param name class name. See {@link #customCompiledCode}
     * @return a class representing the custom compiled class
     * @throws ClassNotFoundException in case the class is not in
     *                                {@link #customCompiledCode}
     */
    protected Class<?> findCustomCompiledCode(String name) throws ClassNotFoundException {
        if (customCompiledCode.containsKey(name)) {
            byte[] byteCode = customCompiledCode.get(name);
            return defineClass(name, byteCode, 0, byteCode.length);
        } else {
            throw new ClassNotFoundException("Custom compiled class '" + name + "' not found.");
        }
    }

    /**
     * Loads the project's classes. This method is the last to be called and is
     * only used for classes in the classpath provided by the user.
     *
     * @param name the project's class name
     * @return a class representing the project's class
     * @throws ClassNotFoundException
     */
    protected Class<?> findProjectClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    /**
     * Stores the results of compilation for custom loading.
     *
     * @param className    the fully qualified class name
     * @param compiledCode the compiled code for the given class
     *
     * @see InMemoryJavaCompiler
     */
    public void setCustomCompiledCode(String className, byte[] compiledCode) {
        this.customCompiledCode.put(className, compiledCode);
    }

    /**
     * Converts a {@code :} separated classpath into an array of
     * {@link URL URLs}.
     *
     * @return the set of converted classpath elements
     */
    private static final URL[] classPathToURLs(String classPath) {

        if (classPath == null) {
            return new URL[0];
        }

        String[] dirs = classPath.split(File.pathSeparator);
        List<URL> urls = new ArrayList<>();

        for (String dir : dirs) {
            try {
                URL url = new File(dir).toURI().toURL();
                urls.add(url);
            } catch (MalformedURLException e) {
                Logger.error("Error converted classpath to URL, malformed: " + dir);
                Logger.error(e);
                System.exit(-1);
            }
        }

        URL[] urlArray = new URL[urls.size()];
        return urls.toArray(urlArray);

    }

    /**
     * Adds multiple {@link URL URLs} as classpath elements to the system
     * classpath and returns the union.
     *
     * @param projectClasspath classpath elements
     * @return new array containing the system classpath and the elements given
     *         as input
     */
    public static final URL[] addSystemClassPath(URL[] projectClasspath) {

        String classPath = System.getProperty("java.class.path");

        String[] paths = classPath.split(File.pathSeparator);

        URL[] urls = new URL[paths.length];
        int counter = 0;

        for (String path : paths) {
            try {
                urls[counter] = new File(path).toURI().toURL();
                counter++;
            } catch (MalformedURLException e) {
                Logger.error("Malformed URL retrieved from system classpath.");
                Logger.error("Cannot initialise CacheClassLoader.");
                Logger.error("URL was: " + path);
                Logger.trace(e);
                System.exit(-1);
            }
        }

        return ArrayUtils.addAll(projectClasspath, urls);

    }

    /**
     * Gets the provided classpath elements provided in the constructor.
     *
     * @return the set of classpath elements given as input to the constructor
     *         of the object
     */
    public URL[] getProvidedClassPath() {
        return providedClassPath;
    }

}
