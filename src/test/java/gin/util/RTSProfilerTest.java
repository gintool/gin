package gin.util;

import gin.TestConfiguration;
import gin.category.LocalTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Giovani
 */
public class RTSProfilerTest {

    private static final String PROJECT_PATH_MAVEN = TestConfiguration.EXAMPLE_DIR_NAME + "ekstazi_mvn";
    private static final File PROJECT_DIR_MAVEN = FileUtils.getFile(PROJECT_PATH_MAVEN);
    private static final String OUTPUT_CSV_PATH_MAVEN = PROJECT_PATH_MAVEN + File.separator + "output_file.csv";

    private static final String PROJECT_PATH_GRADLE = TestConfiguration.EXAMPLE_DIR_NAME + "ekstazi_gradle";
    private static final File PROJECT_DIR_GRADLE = FileUtils.getFile(PROJECT_PATH_GRADLE);
    private static final String OUTPUT_CSV_PATH_GRADLE = PROJECT_PATH_GRADLE + File.separator + "output_file.csv";

    @Test
    public void testMainWithEkstaziMaven() throws IOException {
        String mavenHome = MavenUtils.findMavenHomePath();
        // If maven is not set in the environment path, then this test should
        // not be executed
        Assume.assumeTrue(FileUtils.getFile(mavenHome, "bin/mvn").exists()
                || FileUtils.getFile(mavenHome, "mvn").exists()
                || FileUtils.getFile(mavenHome, "bin/mvn.cmd").exists()
                || FileUtils.getFile(mavenHome, "mvn.cmd").exists());

        String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH_MAVEN, "-rts", "ekstazi", "-o", OUTPUT_CSV_PATH_MAVEN};
        RTSProfiler.main(args);

        File profilerOutDir = FileUtils.getFile(PROJECT_DIR_MAVEN, "profiler_out");
        assertTrue(profilerOutDir.exists());
        assertTrue(profilerOutDir.isDirectory());
        assertEquals(1, profilerOutDir.list().length);
        FileUtils.deleteQuietly(profilerOutDir);

        File ekstaziOut = FileUtils.getFile(PROJECT_DIR_MAVEN, ".ekstazi");
        assertTrue(ekstaziOut.exists());
        assertTrue(ekstaziOut.isDirectory());
        assertEquals(4, ekstaziOut.list().length);
        Assertions.assertAll(Arrays.stream(ekstaziOut.listFiles())
                .map(file -> () -> assertTrue("File " + file.getAbsolutePath() + " is empty.", file.length() > 0)));
        FileUtils.deleteQuietly(ekstaziOut);

        File csvResultFile = FileUtils.getFile(OUTPUT_CSV_PATH_MAVEN);
        assertTrue(csvResultFile.exists());
        FileUtils.deleteQuietly(csvResultFile);
    }

    @Test
    @Category(LocalTest.class)
    public void testMainWithEkstaziGradle() throws IOException {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);
        String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH_GRADLE, "-rts", "ekstazi", "-o", OUTPUT_CSV_PATH_GRADLE};
        RTSProfiler.main(args);

        File profilerOutDir = FileUtils.getFile(PROJECT_DIR_GRADLE, "profiler_out");
        assertTrue(profilerOutDir.exists());
        assertTrue(profilerOutDir.isDirectory());
        assertEquals(1, profilerOutDir.list().length);
        FileUtils.deleteQuietly(profilerOutDir);

        File ekstaziOut = FileUtils.getFile(PROJECT_DIR_GRADLE, ".ekstazi");
        assertTrue(ekstaziOut.exists());
        assertTrue(ekstaziOut.isDirectory());
        assertEquals(4, ekstaziOut.list().length);
        Assertions.assertAll(Arrays.stream(ekstaziOut.listFiles())
                .map(file -> () -> assertTrue(file.length() > 0)));
        FileUtils.deleteQuietly(ekstaziOut);

        File csvResultFile = FileUtils.getFile(OUTPUT_CSV_PATH_GRADLE);
        assertTrue(csvResultFile.exists());
        FileUtils.deleteQuietly(csvResultFile);
    }

    @Test
    public void testWindowsGradleJFR() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);

        String[] args = {"-p", "gradle-simple", "-d", PROJECT_PATH_GRADLE, "-r", "1", "-o", "simple.csv", "-prof", "jfr", "-save", "s"};
        Assert.assertThrows(IllegalArgumentException.class, () -> new Profiler(args));
    }

}
