package gin.test;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CacheClassLoaderTest {

    private static final File resourceDirectory = new File(".." + File.separator + ".." + File.separator + "resources");
    private CacheClassLoader loader;

    @Before
    public void setUp() throws Exception {
        loader = new CacheClassLoader(resourceDirectory.getAbsolutePath());
    }

    @Test
    public void resources() throws MalformedURLException {

        URL[] urls = loader.getURLs();

        // ensure our directory is in there
        URL resourceDirectoryURL = resourceDirectory.toURI().toURL();
        assertTrue(Arrays.asList(urls).contains(resourceDirectoryURL));

    }

    // The special case of JUnitBridge - should load a new class in the cache class loader
    @Test
    public void loadTestRunner() throws ClassNotFoundException {
        Class<InternalTestRunner> systemClassForTestRunner = InternalTestRunner.class;
        Class<?> loadedTestRunner = loader.loadClass("gin.test.JUnitBridge");
        assertNotEquals(systemClassForTestRunner, loadedTestRunner);
    }

    // Now a standard system class

    @Test
    public void loadSystemClass() throws ClassNotFoundException {
        Class<?> patchClass = loader.loadClass("gin.Patch");
        assertEquals(gin.Patch.class, patchClass);
        // and try again
        assertEquals(patchClass, loader.loadClass("gin.Patch"));
    }

    // Non existent class
    @Test(expected = ClassNotFoundException.class)
    public void loadClass() throws ClassNotFoundException {
        loader.loadClass("NonexistentClass");
    }


}
