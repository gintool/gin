package gin.util;

import gin.TestConfiguration;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Assume;
import org.junit.Test;

/**
 *
 * @author Giovani
 */
public class RTSProfilerTest {

    private static final String PROJECT_PATH = TestConfiguration.EXAMPLE_DIR_NAME + "ekstazi_mvn";
    private static final File PROJECT_DIR = FileUtils.getFile(PROJECT_PATH);
    private static final String OUTPUT_CSV_PATH = PROJECT_PATH + File.separator + "output_file.csv";

    @Test
    public void testMainWithEkstazi() {
        String mavenHome = MavenUtils.findMavenHomePath();
        // If maven is not set in the environment path, then this test should
        // not be executed
        Assume.assumeTrue(FileUtils.getFile(mavenHome, "bin/mvn").exists()
                || FileUtils.getFile(mavenHome, "mvn").exists()
                || FileUtils.getFile(mavenHome, "bin/mvn.cmd").exists()
                || FileUtils.getFile(mavenHome, "mvn.cmd").exists());

        String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH, "-rts", "ekstazi", "-o", OUTPUT_CSV_PATH};
        RTSProfiler.main(args);

        assertEquals(1, FileUtils.getFile(PROJECT_DIR, "hprof").list().length);
        assertTrue(FileUtils.getFile(OUTPUT_CSV_PATH).exists());
    }

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
            String[] args = new String[]{"-p", "ekstazi", "-d", PROJECT_PATH, "-rts", "starts", "-o", OUTPUT_CSV_PATH};
            RTSProfiler.main(args);

            assertEquals(1, FileUtils.getFile(PROJECT_DIR, "hprof").list().length);
            assertTrue(FileUtils.getFile(OUTPUT_CSV_PATH).exists());
        } catch (IllegalArgumentException ex) {
            if (SystemUtils.IS_OS_WINDOWS) {
                assertEquals("STARTS will not work on Windows. Please, use 'ekstazi' as an alternative.", ex.getMessage());
            } else {
                Assert.fail(ex.getMessage());
            }
        }
    }

}
