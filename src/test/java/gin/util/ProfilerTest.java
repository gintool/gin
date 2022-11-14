package gin.util;

import gin.TestConfiguration;

import gin.test.UnitTest;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.Assume;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.writers.FileWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class ProfilerTest {

    private final static String GRADLE_SIMPLE_PROJECT_DIR = TestConfiguration.GRADLE_SIMPLE_DIR;
    
    @Test
    public void testEnumProfiling() throws IOException {
        //only run this test if Java version < 9
        Assume.assumeTrue("9".compareTo(System.getProperty("java.version")) > 0);

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "hprof"};

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        UnitTest test = new UnitTest("example.ExampleTest", "profileEnumTest");
        tests.add(test);
        profiler.profileTestSuite(tests); //Use this to generate the profiling file

        File scratchFile = new File("scratch" + File.separator + "testEnumProfiling.txt");
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

        fileWriter.close();
        Files.deleteIfExists(scratchFile.toPath());  // tidy up
        Files.deleteIfExists(new File("scratch").toPath());  // tidy up
    }

    @Test
    public void testJFRProfiling() throws IOException {

        //only run this test if java version >= 8
        Assume.assumeTrue("1.8.0".compareTo(System.getProperty("java.version")) <= 0);

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        UnitTest test = new UnitTest("example.ExampleTest", "jfrPrimeTest");
        tests.add(test);
        profiler.profileTestSuite(tests); //Use this to generate the profiling file

        File scratchFile = new File("scratch" + File.separator + "testEnumProfiling.txt");
        FileWriter fileWriter = new FileWriter(scratchFile.getAbsolutePath());
        Configurator.defaultConfig()
                .writer(fileWriter)
                .level(Level.INFO)
                .activate();

        profiler.parseTraces(tests);

        String logMessages = FileUtils.readFileToString(scratchFile, Charset.defaultCharset());

        String likelyTestMessage = "INFO: Excluding method because class is a test class";
        String primeFunc = "example.ExampleTest.jfrPrimeTest";

        assertTrue(logMessages.contains(likelyTestMessage));
        assertTrue(logMessages.contains(primeFunc));

        fileWriter.close();
        Files.deleteIfExists(scratchFile.toPath());  // tidy up
        Files.deleteIfExists(new File("scratch").toPath());  // tidy up
    }


}
