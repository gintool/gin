package gin.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import com.opencsv.CSVReader;

import gin.TestConfiguration;

public class EmptyPatchTesterTest {

    File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
    File packageDir = new File(resourcesDir, "mypackage");
    File methodFile = new File(packageDir, "profiler_all_results.csv");
    File faultyMethodFile = new File(packageDir, "profiler_faulty_header.csv");

    @Before
    public void setUp() throws Exception {

        buildExampleClasses();
    }

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

    @Test
    public void testSampleMethod() throws Exception {

        File outputFile = new File(packageDir, "empty_patch_results.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.sampleMethods();

        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> lines = reader.readAll();

            assertEquals(lines.size(), 4);

            String[] header = lines.get(0);
            String[] result = lines.get(1);

            int validIndex = Arrays.asList(header).indexOf("PatchValid");
            int compileIndex = Arrays.asList(header).indexOf("PatchCompiled");
            int testIndex = Arrays.asList(header).indexOf("TestPassed");

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);

            result = lines.get(2);

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);

            result = lines.get(3);

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);
        }
        Files.deleteIfExists(outputFile.toPath());  // tidy up

    }

    @Test
    public void testSampleMethodInSubprocess() throws Exception {

        File outputFile = new File(packageDir, "empty_patch_results.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.inSubprocess = true;

        sampler.sampleMethods();

        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> lines = reader.readAll();

            assertEquals(lines.size(), 4);

            String[] header = lines.get(0);
            String[] result = lines.get(1);

            int validIndex = Arrays.asList(header).indexOf("PatchValid");
            int compileIndex = Arrays.asList(header).indexOf("PatchCompiled");
            int testIndex = Arrays.asList(header).indexOf("TestPassed");

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);

            result = lines.get(2);

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);

            result = lines.get(3);

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);
        }
        Files.deleteIfExists(outputFile.toPath());  // tidy up

    }

    @Test
    public void testSampleMethodInNewSubprocess() throws Exception {

        File outputFile = new File(packageDir, "empty_patch_results.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.inNewSubprocess = true;

        sampler.sampleMethods();

        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> lines = reader.readAll();

            assertEquals(lines.size(), 4);

            String[] header = lines.get(0);
            String[] result = lines.get(1);

            int validIndex = Arrays.asList(header).indexOf("PatchValid");
            int compileIndex = Arrays.asList(header).indexOf("PatchCompiled");
            int testIndex = Arrays.asList(header).indexOf("TestPassed");

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);

            result = lines.get(2);

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);

            result = lines.get(3);

            assertEquals("true", result[validIndex]);
            assertEquals("true", result[compileIndex]);
            assertEquals("true", result[testIndex]);
        }
        Files.deleteIfExists(outputFile.toPath());  // tidy up

    }

    @Test
    public void testCreateOutputDirectory() throws Exception {

        File topDir = new File("scratch");
        File innerDir = new File(topDir, "unittest");
        File outputFile = new File(innerDir, "example.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.writeHeader();

        assertTrue(outputFile.exists());
        sampler.close();
        Files.deleteIfExists(outputFile.toPath());
        FileUtils.deleteDirectory(innerDir);
        FileUtils.deleteDirectory(topDir);

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
