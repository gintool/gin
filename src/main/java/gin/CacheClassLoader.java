package gin;

// See https://stackoverflow.com/questions/3971534/how-to-force-java-to-reload-class-upon-instantiation
import java.util.HashMap;

public class CacheClassLoader extends ClassLoader {

    private HashMap<String, Class> cache = new HashMap<>();

    public CacheClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String s) {
        return findClass(s);
    }

    @Override
    public Class<?> findClass(String s) {
        if (cache.containsKey(s)) {
            return cache.get(s);
        } else {
            try {
                return this.getParent().loadClass(s);
            } catch (ClassNotFoundException e) {
                System.err.println("Error dynamically loading class: " + s);
                System.err.println(e);
                return null;
            }
        }
    }

    public void putInCache(String classname, Class klass) {
        cache.put(classname, klass);
    }

}