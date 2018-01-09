package gin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

public class GinClassLoader extends URLClassLoader {

    HashMap<String, Class> cache = new HashMap<>();

    public GinClassLoader(File directory) {
        super(systemClassPath(), null); // Thread.currentThread().getContextClassLoader().getParent());
        try {
            super.addURL(directory.toURI().toURL());
        } catch (MalformedURLException urlException) {
            System.err.println("Class path provided to class loader is invalid");
            System.err.println("Classpath was: " + directory.getAbsolutePath());
            System.exit(-1);
        }
    }

    public static URL[] systemClassPath() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL[] urls = new URL[0];
        if (contextClassLoader instanceof URLClassLoader) {
            urls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
        }
        return urls;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {

        System.out.println("GinClassLoader received a requested for class: " + name);

        if (name.equals("Triangle")) {
            System.out.println("Triangle request received");
        }

        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        try
        {
            Class<?> loaded = super.findLoadedClass(name);
            if( loaded != null )
                return loaded;
            return super.findClass(name);
        }
        catch( ClassNotFoundException e )
        {
            return this.getParent().loadClass(name);
        }
    }

    public void store(String classname, Class<?> klass) {
        this.cache.put(classname, klass);
    }

}
