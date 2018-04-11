package gin.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

/**
 *
 * This classloader intercepts classloading when running unit tests on the software being optimised, and
 * allows us to "overlay" modified classes on the running system.
 *
 * Any other classes are loaded by the system classloader if they're standard Java classes, otherwise
 * if they are non-modified classes belonging to the software being optimised, they will be loaded as normal.
 *
 * There's one extra trick: the IsolatedTestRunner class is loaded by this classloader rather than by the
 * System classloader. That allows Gin to intercept classloading during the actual running of the unit tests.
 * This is because jUnit will use the classloader associated with the class that invoked it.
 * Hence IsolatedTestRunner calls jUnit, jUnit sees that ITR was loaded by this classloader, and hence this
 * classloader becomes the default classloader for that jUnit run.
 *
 */
public class CacheClassLoader extends URLClassLoader {

    /**
     * A cache of compiled classes for a given class name. Used to ensure modified classes are loaded.
     */
    protected HashMap<String, Class> cache = new HashMap<>();

    /**
     * Construct a new class loader that will prioritise an internal cache of classes, and otherwise load from
     * the standard classpath.
     *
     * @param directory
     */
    public CacheClassLoader(File directory) {

        super(systemClassPath(), null);

        try {
            super.addURL(directory.toURI().toURL());
        } catch (MalformedURLException urlException) {
            System.err.println("Class path provided to class loader is invalid");
            System.err.println("Classpath was: " + directory.getAbsolutePath());
            System.exit(-1);
        }

    }

    @Override
    public Class loadClass(String name) throws ClassNotFoundException {

        System.out.println("Load class: " + name);

        // Case (1) Firstly, when the ITR is instantiated, I will load it (via inherited method)
        // To ensure I am recorded as the classloader for IsolatedTestRunner.
        // This means all test executions will load classes via me, so I can intercept them as in case (2)
        if (name.equals("gin.test.IsolatedTestRunner")) {
            return super.loadClass(name);
        }

        // Case (2) Check my cache as modified ("optimised") classes will be in there.
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        // Case (3) Otherwise, it's a system class - use the system loader.
        // Case (4) If the system loader can't find it, then it must be a class that's part of the software being
        // optimised in which case, I load it as I know the path to that software (given in my constructor)
        // If neither work, then we have a missing class / likely typo so let the exception be raised.
        try {
            ClassLoader system = ClassLoader.getSystemClassLoader();
            return system.loadClass(name);
        } catch (ClassNotFoundException e) {
            return super.loadClass(name);
        }

    }

    /**
     * Retrieve current classpath.
     * @return array of URLs containing current classpath.
     */
    protected static URL[] systemClassPath() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] urls = new URL[0];
        if (contextClassLoader instanceof URLClassLoader) {
            urls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
        }
        return urls;
    }

    /**
     * Store a compiled class in the classloader's cache. This will override any classes on disk.
     * @param classname
     * @param klass
     */
    public void store(String classname, Class<?> klass) {
        this.cache.put(classname, klass);
    }

}
