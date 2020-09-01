package gin.util;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Util class to perform actions regarding maven.
 *
 * @author Giovani
 */
public class MavenUtils {

    /**
     * Finds the path to maven home in the System variables, if any. This is the
     * order of preference when searching for it:
     * <ol>
     * <li>{@code MAVEN_HOME}</li>
     * <li>{@code MVN_HOME}</li>
     * <li>The parent directory of a directory (presumably {@code bin}) with a
     * {@code mvn} executable in {@code PATH}</li>
     * <li>{@code /usr/local/}</li>
     * </ol>
     *
     * @return the path to maven home, or {@code /usr/local/} if not found
     *
     * Note: I really don't know how to write tests for this because it can
     * change from one environment to another. Let's just assume it's working :)
     */
    public static String findMavenHomePath() {
        if (System.getenv("MAVEN_HOME") != null) {
            return System.getenv("MAVEN_HOME");
        } else if (System.getenv("MVN_HOME") != null) {
            return System.getenv("MVN_HOME");
        } else if (System.getenv("PATH") != null) {
            String systemPath = System.getenv("PATH");
            String[] split = StringUtils.split(systemPath, File.pathSeparator);
            for (String pathItem : split) {
                File parentDir = FileUtils.getFile(pathItem);
                if (FileUtils.getFile(parentDir, "mvn").exists()
                        || FileUtils.getFile(parentDir, "mvn.cmd").exists()) {
                    return parentDir.getParentFile().getAbsolutePath();
                }
            }
        }
        return "/usr/local/";
    }

    /**
     * Finds the {@link File} to maven home in the System variables, if any.
     * This is the order of preference when searching for it:
     * <ol>
     * <li>{@code MAVEN_HOME}</li>
     * <li>{@code MVN_HOME}</li>
     * <li>The parent directory of a directory (presumably {@code bin}) with a
     * {@code mvn} executable in {@code PATH}</li>
     * <li>{@code /usr/local/}</li>
     * </ol>
     *
     * @return the File to maven home, or {@code /usr/local/} if not found
     *
     */
    public static File findMavenHomeFile() {
        return FileUtils.getFile(findMavenHomePath());
    }

}
