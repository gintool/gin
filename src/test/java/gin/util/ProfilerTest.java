package gin.util;

import gin.TestConfiguration;

import gin.test.UnitTest;
import org.apache.commons.io.FileUtils;
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

        String[] args = {"-p", "gradle-simple", "-d", GRADLE_SIMPLE_PROJECT_DIR, "-r", "1", "-o", "simple.csv"};

        Profiler profiler = new Profiler(args);
        Set<UnitTest> tests = new HashSet<>();
        UnitTest test = new UnitTest("example.ExampleTest", "profileEnumTest");
        tests.add(test);
        //profiler.profileTestSuite(tests); //Use this to generate the hprof

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

}
