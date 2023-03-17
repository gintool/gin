package gin.test;

import org.apache.commons.lang3.ArrayUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intercept classloading of JUnitBridge, and provide access to class from the
 * target system Also allow overlaying of the modified classes.F
 */
public class CacheClassLoader extends URLClassLoader implements Serializable {

    @Serial
    private static final long serialVersionUID = -9181563170107783961L;

    private static final String BRIDGE_CLASS_NAME = gin.test.JUnitBridge.class.getName();
    private final URL[] providedClassPath;
    protected Map<String, byte[]> customCompiledCode = new HashMap<>();

    /**
     * Constructs a ClassLoader with the system classpath and the elements given
     * as input.
     *
     * @param classPaths classpath elements to append to the system's classpath.
     */
    public CacheClassLoader(URL[] classPaths) {
        super(addSystemClassPath(classPaths), null);
        providedClassPath = classPaths;
    }

    /**
     * Constructs a ClassLoader with the system classpath and the elements given
     * as input. The input should be a full classpath with elements separated by
     * a colon.
     *
     * @param classpath a : separated classpath to append to the system's
     *                  classpath.
     */
    public CacheClassLoader(String classpath) {
        this(classPathToURLs(classpath));
    }

    /**
     * Utility method to convert a : separated classpath into an array of URLs.
     *
     * @return the array of converted classpath elements.
     */
    public static URL[] classPathToURLs(String classPath) {

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
     * Add multiple URLs as classpath elements to the system classpath and
     * returns the union.
     *
     * @param projectClasspath classpath elements.
     * @return new array containing the system classpath and the elements given
     * as input.
     */
    public static URL[] addSystemClassPath(URL[] projectClasspath) {

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
     * If the class can't be found using parents (I don't have any) then drops
     * back here.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        // I have to intervene here, to ensure JUnitBridge uses me in the future.
        if (name.equals(BRIDGE_CLASS_NAME)) {
            return super.findClass(name);
        }

        // Modified class? Return the modified code.
        if (customCompiledCode.containsKey(name)) {
            byte[] byteCode = customCompiledCode.get(name);
            return defineClass(name, byteCode, 0, byteCode.length);
        }

        // Otherwise, try the system class loader. If not there, must be part of the project, so load ourselves.
        try {
            ClassLoader system = ClassLoader.getSystemClassLoader();
            return system.loadClass(name);
        } catch (ClassNotFoundException e) {
            return super.findClass(name);
        }

    }

    /**
     * Store the results of compilation from the InMemoryCompiler.
     *
     * @param className    the fully qualified class name.
     * @param compiledCode the compiled code for the given class.
     */
    public void setCustomCompiledCode(String className, byte[] compiledCode) {
        this.customCompiledCode.put(className, compiledCode);
    }

    /**
     * Gets the provided classpath elements provided in the constructor;
     *
     * @return the array of classpath elements given as input to the constructor
     * of the object.
     */
    public URL[] getProvidedClassPath() {
        return providedClassPath;
    }

}
