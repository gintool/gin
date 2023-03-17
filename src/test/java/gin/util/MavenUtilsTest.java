package gin.util;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * @author Giovani
 */
public class MavenUtilsTest {

    public MavenUtilsTest() {
    }

    /**
     * Because this test depends on the context of the system, I did not add any
     * assertion here. The only assertion is that it should not be null. It
     * should fallback to {@code /usr/share} in case nothing is found. "Assume"
     * will skip the test in case maven is not configured in the system
     * environment.
     */
    @Test
    public void testFindMavenHomePath() {
        String mavenHome = MavenUtils.findMavenHomePath();
        Assert.assertNotNull(mavenHome);
        Assume.assumeTrue(FileUtils.getFile(mavenHome, "bin/mvn").exists()
                || FileUtils.getFile(mavenHome, "mvn").exists()
                || FileUtils.getFile(mavenHome, "bin/mvn.cmd").exists()
                || FileUtils.getFile(mavenHome, "mvn.cmd").exists());
    }

}
