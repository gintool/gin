package gin.test;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CacheClassLoaderTest {

    private static final File resourceDirectory = new File("../../resources");
    private CacheClassLoader loader;

    @Before
    public void setUp() throws Exception {
        loader = new CacheClassLoader(resourceDirectory);
    }

    @Test
    public void resources() throws MalformedURLException {

        loader = new CacheClassLoader(resourceDirectory);
        URL[] urls = loader.getURLs();

        // ensure our directory is in there
        URL resourceDirectoryURL = resourceDirectory.toURI().toURL();
        assertTrue(Arrays.asList(urls).contains(resourceDirectoryURL));

        // ensure thread path directories are also in there
        assert(Thread.currentThread().getContextClassLoader() instanceof URLClassLoader);
        URLClassLoader threadLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
        URL[] threadURLS = threadLoader.getURLs();
        for (URL url : threadURLS) {
            assertTrue(Arrays.asList(urls).contains(url));
        }

    }

    @Test
    public void systemClassPath() {
        URL[] urls = CacheClassLoader.systemClassPath();
        assert(Thread.currentThread().getContextClassLoader() instanceof URLClassLoader);
        URLClassLoader threadLoader = (URLClassLoader)Thread.currentThread().getContextClassLoader();
        URL[] threadURLS = threadLoader.getURLs();
        for (URL url : threadURLS) {
            assertTrue(Arrays.asList(urls).contains(url));
        }
        for (URL url : urls) {
            assertTrue(Arrays.asList(threadURLS).contains(url));
        }
    }

    @Test
    public void store() {
        loader.store("ExampleClass", this.getClass());
        assertTrue(loader.cache.containsKey("ExampleClass"));
        assertEquals(loader.cache.get("ExampleClass"), this.getClass());
    }


    @Test(expected = ClassNotFoundException.class)
    public void loadClass() throws ClassNotFoundException {
        loader.loadClass("NonexistentClass");
    }

    @Test
    public void findClassInCache() throws ClassNotFoundException {
        loader.store("ExampleClass", this.getClass());
        assertEquals(this.getClass(), loader.loadClass("ExampleClass"));
    }

    @Test
    public void findClassInHierarchy() throws ClassNotFoundException {
        Class patchClass = loader.loadClass("gin.Patch");
        // this will have loaded the class separately from the system
        assertNotEquals(gin.Patch.class, patchClass);
        // But should be internally consistent
        assertEquals(patchClass, loader.loadClass("gin.Patch"));
    }

}