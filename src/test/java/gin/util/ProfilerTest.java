package gin.util;

import gin.TestConfiguration;
import gin.category.LocalTest;
import gin.test.UnitTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.writers.FileWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfilerTest {

    private final static String GRADLE_SIMPLE_PROJECT_DIR = TestConfiguration.GRADLE_SIMPLE_DIR;

    @Test
    public void testHPROFProfilingGradleJUnit4() throws IOException {
        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "hprof", "-save", "s"};
        this.testHprof(args);
    }

    @Test
    public void testHPROFProfilingGradleJUnit5() throws IOException {
        String[] args = {"-p", "gradle-simple-junit5", "-d", TestConfiguration.GRADLE_SIMPLE_JUNIT5_DIR, "-r", "1", "-o", "simple.csv", "-prof", "hprof", "-save", "s"};
        this.testHprof(args);
    }

    public void testHprof(String[] args) throws IOException {
        //only run this test if Java version < 9
        Assume.assumeTrue(JavaUtils.getJavaVersion() < 9);

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        UnitTest test = new UnitTest("example.ExampleTest", "profileEnumTest");
        tests.add(test);
        Map<UnitTest, Profiler.ProfileResult> unitTestProfileResult = profiler.profileTestSuite(tests);//Use this to generate the profiling file

        File scratchFile = new File("scratchhprof" + File.separator + "testEnumProfiling.txt");
        FileWriter fileWriter = new FileWriter(scratchFile.getAbsolutePath());
        Configurator.defaultConfig()
                .writer(fileWriter)
                .level(Level.WARNING)
                .activate();

        profiler.parseTraces(tests);

        String logMessages = FileUtils.readFileToString(scratchFile, Charset.defaultCharset());

        String missingMessage = "WARNING: Excluding method as class in main tree but method not found: "
                + "example.ExampleEnum.values";

        String likelyEnumMessage = "WARNING: This is likely because the method relates to an enum type.";

        assertTrue(logMessages.contains(missingMessage));
        assertTrue(logMessages.contains(likelyEnumMessage));

        // Ensures the results of all test cases when executed by the profiler is a success
        assertFalse(unitTestProfileResult.isEmpty());
        Assertions.assertAll(unitTestProfileResult.values().stream().map(result -> () -> assertTrue(result.success)));

        fileWriter.close();
        Files.deleteIfExists(scratchFile.toPath());  // tidy up
        Files.deleteIfExists(new File("scratchhprof").toPath());  // tidy up
    }

    @Test
    @Category(LocalTest.class)
    public void testJFRProfilingGradleJUnit4() throws IOException {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};
        UnitTest test = new UnitTest("example.ExampleTest", "jfrPrimeTest");
        this.testJFR(args, test);
    }

    @Test
    @Category(LocalTest.class)
    public void testJFRProfilingGradleJUnit5() throws IOException {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

        String[] args = {"-p", "gradle-simple-junit5", "-d", TestConfiguration.GRADLE_SIMPLE_JUNIT5_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};
        UnitTest test = new UnitTest("example.ExampleTest", "jfrPrimeTest");
        this.testJFR(args, test);
    }

    @Test
    public void testJFRProfilingMavenJUnit4() throws IOException {
        String[] args = {"-p", "maven-simple", "-d", TestConfiguration.MAVEN_SIMPLE_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};
        UnitTest test = new UnitTest("com.mycompany.app.AppTest", "jfrPrimeTest");
        this.testJFR(args, test);
    }

    @Test
    public void testJFRProfilingMavenJUnit5() throws IOException {
        String[] args = {"-p", "maven-simple-junit5", "-d", TestConfiguration.MAVEN_SIMPLE_JUNIT5_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};
        UnitTest test = new UnitTest("com.mycompany.app.AppTest", "jfrPrimeTest");
        this.testJFR(args, test);
    }

    public void testJFR(String[] args, UnitTest test) throws IOException {
        //only run this test if java version >= 8
        Assume.assumeTrue(JavaUtils.getJavaVersion() > 8);

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        tests.add(test);
        Map<UnitTest, Profiler.ProfileResult> unitTestProfileResult = profiler.profileTestSuite(tests);//Use this to generate the profiling file

        File scratchFile = new File("scratchjfr" + File.separator + "testJFRProfiling.txt");
        FileWriter fileWriter = new FileWriter(scratchFile.getAbsolutePath());
        Configurator.defaultConfig()
                .writer(fileWriter)
                .level(Level.INFO)
                .activate();

        profiler.parseTraces(tests);

        String logMessages = FileUtils.readFileToString(scratchFile, Charset.defaultCharset());

        String primeFunc = "INFO: Parsing trace for test: " + test.getTestName() + " []";
        assertTrue(logMessages.contains(primeFunc));
        assertFalse(StringUtils.containsIgnoreCase(logMessages, "exception"));

        // Ensures the results of all test cases when executed by the profiler is a success
        assertFalse(unitTestProfileResult.isEmpty());
        Assertions.assertAll(unitTestProfileResult.values().stream().map(result -> () -> assertTrue(result.success)));

        fileWriter.close();
        Files.deleteIfExists(scratchFile.toPath());  // tidy up
        Files.deleteIfExists(new File("scratchjfr").toPath());  // tidy up
    }

    @Test
    public void testWindowsGradleJFR() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};
        Assert.assertThrows(IllegalArgumentException.class, () -> new Profiler(args));
    }

}
