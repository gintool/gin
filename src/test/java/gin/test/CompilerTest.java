package gin.test;

import gin.test.classloader.CacheClassLoader;
import gin.TestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mdkt.compiler.CompiledCode;


import static org.junit.Assert.*;

public class CompilerTest {

    private CacheClassLoader loader;

    private final static String EXAMPLE_DIR_NAME = TestConfiguration.EXAMPLE_DIR_NAME;

    @Before
    public void setUp() {
        loader = new CacheClassLoader(EXAMPLE_DIR_NAME);
    }

    @Test
    public void testCompile() throws ClassNotFoundException {

        String classPath = EXAMPLE_DIR_NAME;
        String className = "SimpleExample";

        CompiledCode code = Compiler.compile(className, "public class SimpleExample {} ", classPath);

        assertTrue(code != null);

        loader.setCustomCompiledCode(className, code.getByteCode());
        Class compiledClass = loader.loadClass("SimpleExample");

        assertNotNull(compiledClass);
        assertEquals("SimpleExample", compiledClass.getSimpleName());
    }

}
