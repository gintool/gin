package gin.test.classloader;

import java.net.URLClassLoader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Giovani
 */
public class ClassLoaderFactoryTest {

    public ClassLoaderFactoryTest() {
    }

    @Test
    public void testCreateClassLoader() {
        ClassLoaderFactory classLoaderFactory = ClassLoaderFactory.createDefaultGinClassLoader();
        assertEquals(GinClassLoader.CACHE_CLASSLOADER, classLoaderFactory.getClassLoader());
        URLClassLoader classLoader = classLoaderFactory.createClassLoader("foo/bar");
        assertTrue(classLoader instanceof CacheClassLoader);
    }

}
