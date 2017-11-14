package gin;

// See https://stackoverflow.com/questions/3971534/how-to-force-java-to-reload-class-upon-instantiation

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

public class CacheClassLoader extends ClassLoader {

    private HashMap<String, Class> cache = new HashMap<>();

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

    public void setClass(String classname, Class klass) {
            cache.put(classname, klass);
    }


//    private byte[] loadClassData(String className) throws IOException {
//        File f = new File("./tmp/" + className.replaceAll("\\.", "/") + ".class");
//        int size = (int) f.length();
//        byte buff[] = new byte[size];
//        FileInputStream fis = new FileInputStream(f);
//        DataInputStream dis = new DataInputStream(fis);
//        dis.readFully(buff);
//        dis.close();
//        return buff;
//    }
}