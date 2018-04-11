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

    // The special case of IsolatedTestRunner - should load a new class in the cache class loader
    @Test
    public void loadTestRunner() throws ClassNotFoundException {
        Class systemClassForTestRunner = TestRunner.class;
        Class loadedTestRunner = loader.loadClass("gin.test.IsolatedTestRunner");
        assertNotEquals(systemClassForTestRunner, loadedTestRunner);
    }

    // Now a standard system class

    @Test
    public void loadSystemClass() throws ClassNotFoundException {
        Class patchClass = loader.loadClass("gin.Patch");
        assertEquals(gin.Patch.class, patchClass);
        // and try again
        assertEquals(patchClass, loader.loadClass("gin.Patch"));
    }

    // Next, a class that has been modified by gin and stored in the cache
    @Test
    public void loadClassFromCache() throws ClassNotFoundException {
        loader.store("ExampleClass", this.getClass());
        assertEquals(this.getClass(), loader.loadClass("ExampleClass"));
    }

    // Non existent class
    @Test(expected = ClassNotFoundException.class)
    public void loadClass() throws ClassNotFoundException {
        loader.loadClass("NonexistentClass");
    }


}