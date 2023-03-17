package gin.util;

import com.opencsv.CSVReader;
import gin.TestConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RandomSamplerTest {

    File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
    File packageDir = new File(resourcesDir, "mypackage");
    File methodFile = new File(packageDir, "profiler_results.csv");
    File outputFile = new File(packageDir, "random_sampler_results.csv");

    RandomSampler sampler;

    // Compile source files.
    private static void buildExampleClasses() throws IOException {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = new ArrayList<>();
            options.add("-cp");
            options.add(System.getProperty("java.class.path"));
            File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
            resourcesDir = new File(resourcesDir, "mypackage");
            File exampleFile = new File(resourcesDir, "Example.java");
            File exampleTestFile = new File(resourcesDir, "ExampleTest.java");
            File exampleBaseFile = new File(resourcesDir, "ExampleBase.java");
            Iterable<? extends JavaFileObject> compilationUnit = fm.getJavaFileObjectsFromFiles(Arrays.asList(
                    exampleFile,
                    exampleTestFile,
                    exampleBaseFile
            ));
            JavaCompiler.CompilationTask task
                    = compiler.getTask(null, fm, null, options, null, compilationUnit);
            if (!task.call()) {
                throw new AssertionError("compilation failed");
            }
        }

    }

    @Before
    public void setUp() throws Exception {

        sampler = new RandomSampler(resourcesDir, methodFile);
        sampler.outputFile = outputFile;
        sampler.classPath = resourcesDir.getPath();
        sampler.patchSize = 2;
        sampler.setUp();

        buildExampleClasses();
    }

    @Test
    public void testApplyPatchesToMethod() throws Exception {

        sampler.sampleMethods();

        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> lines = reader.readAll();

            assertEquals(lines.size(), 11);

            String[] header = lines.get(0);
            String[] result = lines.get(1);

            int validIndex = Arrays.asList(header).indexOf("PatchValid");
            int compileIndex = Arrays.asList(header).indexOf("PatchCompiled");
            int testIndex = Arrays.asList(header).indexOf("TestPassed");
            int patchSize = Arrays.asList(header).indexOf("PatchSize");

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(2);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(3);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(4);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(5);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(6);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(7);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(8);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(9);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);

            result = lines.get(10);

            assertEquals("true", result[validIndex]);
            assertEquals("false", result[compileIndex]);
            assertEquals("false", result[testIndex]);
            assertEquals("2", result[patchSize]);
        }
        Files.deleteIfExists(outputFile.toPath());  // tidy up

    }

    @After
    public void tearDown() throws Exception {
        File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
        resourcesDir = new File(resourcesDir, "mypackage");
        Files.deleteIfExists(new File(resourcesDir, "Example.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ExampleBase.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ExampleTest.class").toPath());
    }
}
