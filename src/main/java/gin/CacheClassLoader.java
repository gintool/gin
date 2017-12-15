package gin;

// See https://stackoverflow.com/questions/3971534/how-to-force-java-to-reload-class-upon-instantiation
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CacheClassLoader extends URLClassLoader {

    private HashMap<String, Class> cache = new HashMap<>();
    private Set<String> whiteList = new HashSet<>();

    public CacheClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String s) {
        return findClass(s);
    }

    @Override
    public Class<?> findClass(String s) {
        System.out.println("Request to load: " + s);
        if (cache.containsKey(s)) {
            return cache.get(s);
        } else if (whiteList.contains(s)) {
            try {
                return super.findClass(s);
            } catch (ClassNotFoundException e) {
                System.err.println("Error dynamically loading class from URLs: " + s);
                System.err.println(e);
                return null;
            }
        } else {
            try {
                return ClassLoader.getSystemClassLoader().loadClass(s);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.err.println("Error using system class loader to get class: " + s);
                return null;
            }
        }
    }

    public void putInCache(String classname, Class klass) {
        cache.put(classname, klass);
    }

    public void addToWhiteList(String classname) {
        whiteList.add(classname);
    }
}