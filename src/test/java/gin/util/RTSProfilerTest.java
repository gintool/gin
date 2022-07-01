package gin.util;

import gin.TestConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;

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
    public void testMainWithEkstaziMaven() {
        String mavenHome = MavenUtils.findMavenHomePath();
        // If maven is not set in the environment path, then this test should
        // not be executed
        Assume.assumeTrue(FileUtils.getFile(mavenHome, "bin/mvn").exists()
                || FileUtils.getFile(mavenHome, "mvn").exists()
                || FileUtils.getFile(mavenHome, "bin/mvn.cmd").exists()
                || FileUtils.getFile(mavenHome, "mvn.cmd").exists());

        String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH_MAVEN, "-rts", "ekstazi", "-o", OUTPUT_CSV_PATH_MAVEN};
        RTSProfiler.main(args);

        assertEquals(1, FileUtils.getFile(PROJECT_DIR_MAVEN, "hprof").list().length);
        assertTrue(FileUtils.getFile(OUTPUT_CSV_PATH_MAVEN).exists());
    }

//    @Test
//    public void testMainWithEkstaziGradle() {
//        String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH_GRADLE, "-rts", "ekstazi", "-o", OUTPUT_CSV_PATH_GRADLE};
//        RTSProfiler.main(args);
//
//        assertEquals(1, FileUtils.getFile(PROJECT_DIR_GRADLE, "hprof").list().length);
//        assertTrue(FileUtils.getFile(OUTPUT_CSV_PATH_GRADLE).exists());
//    }

    @Test
    public void testMainWithSTARTS() {
        String mavenHome = MavenUtils.findMavenHomePath();
        // If maven is not set in the environment path, then this test should
        // not be executed
        Assume.assumeTrue(FileUtils.getFile(mavenHome, "bin/mvn").exists()
                || FileUtils.getFile(mavenHome, "mvn").exists()
                || FileUtils.getFile(mavenHome, "bin/mvn.cmd").exists()
                || FileUtils.getFile(mavenHome, "mvn.cmd").exists());

        try {
            String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH_MAVEN, "-rts", "starts", "-o", OUTPUT_CSV_PATH_MAVEN};
            RTSProfiler.main(args);

            assertEquals(1, FileUtils.getFile(PROJECT_DIR_MAVEN, "hprof").list().length);
            assertTrue(FileUtils.getFile(OUTPUT_CSV_PATH_MAVEN).exists());
        } catch (IllegalArgumentException ex) {
            if (SystemUtils.IS_OS_WINDOWS) {
                assertEquals("STARTS will not work on Windows. Please, use 'ekstazi' as an alternative.", ex.getMessage());
            } else {
                Assert.fail(ex.getMessage());
            }
        }
    }
}
