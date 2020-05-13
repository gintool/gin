package gin.test;

import gin.TestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mdkt.compiler.CompiledCode;

import java.io.File;

import static org.junit.Assert.*;

public class CompilerTest {

    private CacheClassLoader loader;

    private final static File exampleDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
    private final static String exampleDirName = exampleDir.getPath();

    @Before
    public void setUp() {
        loader = new CacheClassLoader(exampleDirName);
    }

    @Test
    public void testCompile() throws ClassNotFoundException {

        String classPath = exampleDirName;
        String className = "SimpleExample";

        CompiledCode code = Compiler.compile(className, "public class SimpleExample {} ", classPath);

        assertTrue(code != null);

        CacheClassLoader loader = new CacheClassLoader(classPath);
        loader.setCustomCompiledCode(className, code.getByteCode());
        Class compiledClass = loader.findClass("SimpleExample");

        assertNotNull(compiledClass);
        assertEquals("SimpleExample", compiledClass.getSimpleName());
    }

}