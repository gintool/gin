package gin.util;

import gin.TestConfiguration;
import gin.test.UnitTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.writers.FileWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ProfilerTest {

    private final static String GRADLE_SIMPLE_PROJECT_DIR = TestConfiguration.GRADLE_SIMPLE_DIR;

    @Test
    public void testEnumProfiling() throws IOException {
        //only run this test if Java version < 9
        Assume.assumeTrue(JavaUtils.getJavaVersion() < 9);

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "hprof", "-save", "s"};

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        UnitTest test = new UnitTest("example.ExampleTest", "profileEnumTest");
        tests.add(test);
        profiler.profileTestSuite(tests); //Use this to generate the profiling file

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

        fileWriter.close();
        Files.deleteIfExists(scratchFile.toPath());  // tidy up
        Files.deleteIfExists(new File("scratchhprof").toPath());  // tidy up
    }

    @Test
    public void testJFRProfiling() throws IOException {

        //only run this test if java version >= 8
        Assume.assumeTrue(JavaUtils.getJavaVersion() > 8);

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        UnitTest test = new UnitTest("example.ExampleTest", "jfrPrimeTest");
        tests.add(test);
        profiler.profileTestSuite(tests); //Use this to generate the profiling file

        File scratchFile = new File("scratchjfr" + File.separator + "testJFRProfiling.txt");
        FileWriter fileWriter = new FileWriter(scratchFile.getAbsolutePath());
        Configurator.defaultConfig()
                .writer(fileWriter)
                .level(Level.INFO)
                .activate();

        profiler.parseTraces(tests);

        String logMessages = FileUtils.readFileToString(scratchFile, Charset.defaultCharset());

        String primeFunc = "INFO: Parsing trace for test: example.ExampleTest.jfrPrimeTest []";

        assertTrue(logMessages.contains(primeFunc));

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

//    @Test
//    public void testGradleConnector() {
//        GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(FileUtils.getFile(GRADLE_SIMPLE_PROJECT_DIR));
//        connector.useGradleVersion("8.0.2");
//        ProjectConnection connection = connector.connect();
//        TestLauncher testLauncher = connection.newTestLauncher();
//        testLauncher.withJvmTestMethods("example.ExampleTest", "jfrPrimeTest");
//        Map<String, String> variables = new HashMap<>();
//        variables.put("JAVA_TOOL_OPTIONS", "-XX:+FlightRecorder -XX:StartFlightRecording=name=Gin,dumponexit=true,settings=profile,filename=./test.jfr");
//        testLauncher.setEnvironmentVariables(variables);
//        try {
//            testLauncher.run();
//        } catch (TestExecutionException | BuildException exception) {
//            Logger.error(exception.getClass().getSimpleName() + " from gradle test launcher");
//            Logger.error("Message: " + exception.getMessage());
//            Logger.error("Cause: " + exception.getCause());
//            exception.printStackTrace();
//            System.exit(-1);
//        }
//        connection.close();
//    }

}
