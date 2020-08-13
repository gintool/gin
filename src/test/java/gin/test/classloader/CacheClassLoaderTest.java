package gin.test.classloader;

import gin.test.InternalTestRunner;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CacheClassLoaderTest {

    private static final File RESOURCE_DIRECTORY = new File(".." + File.separator + ".." + File.separator + "resources");
    private final static String TRIANGLE_DIR = "." + File.separator + "examples" + File.separator + "triangle";
    private CacheClassLoader loader;

    @Before
    public void setUp() throws Exception {
        loader = new CacheClassLoader(RESOURCE_DIRECTORY.getAbsolutePath());
    }

    @Test
    public void resources() throws MalformedURLException {

        URL[] urls = loader.getURLs();

        // ensure our directory is in there
        URL resourceDirectoryURL = RESOURCE_DIRECTORY.toURI().toURL();
        assertTrue(Arrays.asList(urls).contains(resourceDirectoryURL));

    }

    // The special case of JUnitBridge - should load a new class in the cache class loader
    @Test
    public void loadTestRunner() throws ClassNotFoundException {
        Class systemClassForTestRunner = InternalTestRunner.class;
        Class loadedTestRunner = loader.loadClass("gin.test.JUnitBridge");
        assertNotEquals(systemClassForTestRunner, loadedTestRunner);
        assertEquals("gin.test.JUnitBridge", loadedTestRunner.getName());
    }

    // The special case of JUnitBridge - should load a new class in the cache class loader
    @Test
    public void loadTestRunner2() throws ClassNotFoundException {
        Class systemClassForTestRunner = InternalTestRunner.class;
        Class loadedTestRunner = loader.findBridgeClass("gin.test.JUnitBridge");
        assertNotEquals(systemClassForTestRunner, loadedTestRunner);
        assertEquals("gin.test.JUnitBridge", loadedTestRunner.getName());
    }

    // Now a standard system class
    @Test
    public void loadSystemClass() throws ClassNotFoundException {
        Class patchClass = loader.loadClass("gin.Patch");
        assertEquals(gin.Patch.class, patchClass);
        // and try again
        assertEquals(patchClass, loader.loadClass("gin.Patch"));
    }

    @Test
    public void loadProjectClass() throws ClassNotFoundException {
        loader = new CacheClassLoader(TRIANGLE_DIR);
        Class loadedTestRunner = loader.findProjectClass("Triangle");
        assertNotNull(loadedTestRunner);
        assertEquals("Triangle", loadedTestRunner.getName());
    }

    @Test(expected = ClassNotFoundException.class)
    public void failCustomCompiledCode() throws ClassNotFoundException {
        Class loadedTestRunner = loader.findCustomCompiledCode("Triangle");
    }

    // Non existent class
    @Test(expected = ClassNotFoundException.class)
    public void loadClass() throws ClassNotFoundException {
        loader.loadClass("NonexistentClass");
    }

}
