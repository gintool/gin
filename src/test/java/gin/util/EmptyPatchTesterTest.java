package gin.util;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import gin.TestConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

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
import static org.junit.Assert.assertTrue;

public class EmptyPatchTesterTest {

    File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
    File packageDir = new File(resourcesDir, "mypackage");
    File methodFile = new File(packageDir, "profiler_all_results.csv");

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

    private static void validateOutputFile(File outputFile) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(outputFile))) {
            List<String[]> lines = reader.readAll();

            assertTrue(lines.size() > 0);

            String[] header = lines.get(0);

            int validIndex = Arrays.asList(header).indexOf("PatchValid");
            int compileIndex = Arrays.asList(header).indexOf("PatchCompiled");
            int testIndex = Arrays.asList(header).indexOf("TestPassed");

            lines.remove(0);

            // Test whether all lines are true for all important stuff
            Assertions.assertAll(lines.stream().map(line -> () -> {
                assertEquals("Test not valid", "true", line[validIndex]);
                assertEquals("Test not compiling", "true", line[compileIndex]);
                assertEquals("Test not passing", "true", line[testIndex]);
            }));
        }
        Files.deleteIfExists(outputFile.toPath());  // tidy up
    }

    @Test
    public void testSampleMethod() throws Exception {
        buildExampleClasses();
        File outputFile = new File(packageDir, "empty_patch_results.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.sampleMethods();

        validateOutputFile(outputFile);

        tearDownExampleCLasses();
    }

    @Test
    public void testSampleMethodInSubprocess() throws Exception {
        buildExampleClasses();
        File outputFile = new File(packageDir, "empty_patch_results.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.inSubprocess = true;

        sampler.sampleMethods();

        validateOutputFile(outputFile);

        tearDownExampleCLasses();
    }

    @Test
    public void testSampleMethodInNewSubprocess() throws Exception {
        buildExampleClasses();
        File outputFile = new File(packageDir, "empty_patch_results.csv");

        EmptyPatchTester sampler = new EmptyPatchTester(resourcesDir, methodFile);
        sampler.classPath = resourcesDir.getPath();
        sampler.outputFile = outputFile;
        sampler.setUp();
        sampler.eachTestInNewSubprocess = true;

        sampler.sampleMethods();

        validateOutputFile(outputFile);

        tearDownExampleCLasses();
    }

    @Test
    public void testSampleMethodMavenJUnit4() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.MAVEN_SIMPLE_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath()};

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodInSubprocessMavenJUnit4() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.MAVEN_SIMPLE_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath(),
                "-j"
        };

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodMavenJUnit5() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.MAVEN_SIMPLE_JUNIT5_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath()};

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodInSubprocessMavenJUnit5() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.MAVEN_SIMPLE_JUNIT5_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath(),
                "-j"
        };

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodGradleJUnit4() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.GRADLE_SIMPLE_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath()};

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodInSubprocessGradleJUnit4() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.GRADLE_SIMPLE_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath(),
                "-j"
        };

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodGradleJUnit5() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.GRADLE_SIMPLE_JUNIT5_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath()};

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testSampleMethodInSubprocessGradleJUnit5() throws Exception {
        File projectDir = FileUtils.getFile(TestConfiguration.GRADLE_SIMPLE_JUNIT5_DIR);
        File method_file = FileUtils.getFile(projectDir, "profiler_results_for_testing.csv");
        File outputFile = FileUtils.getFile(projectDir, "empty_patch_results.csv");

        String[] args = new String[]{"-p", "maven-simple",
                "-d", projectDir.getAbsolutePath(),
                "-m", method_file.getAbsolutePath(),
                "-o", outputFile.getAbsolutePath(),
                "-j"
        };

        EmptyPatchTester.main(args);

        validateOutputFile(outputFile);
    }

    @Test
    public void testCreateOutputDirectory() throws Exception {
        buildExampleClasses();
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
        tearDownExampleCLasses();
    }

    public void tearDownExampleCLasses() throws IOException {
        File resourcesDir = new File(TestConfiguration.EXAMPLE_DIR_NAME);
        resourcesDir = new File(resourcesDir, "mypackage");
        Files.deleteIfExists(new File(resourcesDir, "Example.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ExampleBase.class").toPath());
        Files.deleteIfExists(new File(resourcesDir, "ExampleTest.class").toPath());
    }
}
