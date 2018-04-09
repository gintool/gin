package gin.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

public class CacheClassLoader extends URLClassLoader {

    /**
     * A cache of compiled classes for a given class name. Used to ensure modified classes are loaded.
     */
    private HashMap<String, Class> cache = new HashMap<>();

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

    /**
     * Retrieve current classpath.
     * @return array of URLs containing current classpath.
     */
    private static URL[] systemClassPath() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] urls = new URL[0];
        if (contextClassLoader instanceof URLClassLoader) {
            urls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
        }
        return urls;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {

        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        try {
            Class<?> loaded = super.findLoadedClass(name);
            if(loaded != null) {
                return loaded;
            }
            return super.findClass(name);
        } catch(ClassNotFoundException e) {
            return this.getParent().loadClass(name);
        }

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
