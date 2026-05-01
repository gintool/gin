package gin.test;

import gin.TestConfiguration;
import org.junit.Test;
import org.mdkt.compiler.CompiledCode;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CompilerTest {

    private final static File exampleDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
    private final static String exampleDirName = exampleDir.getPath();

    @Test
    public void testCompile() throws ClassNotFoundException, IOException {

        String classPath = exampleDirName;
        String className = "SimpleExample";

        CompiledCode code = Compiler.compile(className, "public class SimpleExample {} ", classPath);

        assertNotNull(code);

        Class<?> compiledClass;
        try (CacheClassLoader loader = new CacheClassLoader(classPath)) {
            loader.setCustomCompiledCode(className, code.getByteCode());
            compiledClass = loader.findClass("SimpleExample");
        }

        assertNotNull(compiledClass);
        assertEquals("SimpleExample", compiledClass.getSimpleName());
    }

}