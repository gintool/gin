package gin.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/**
 * Util class to perform actions regarding maven.
 *
 * @author Giovani
 */
public class MavenUtils implements Serializable {

    @Serial
    private static final long serialVersionUID = 6646703137441264344L;

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
     * <p>
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
        throw new RuntimeException("Gin could not locate the maven executable. Please, set your 'MAVEN_HOME' environment variable, or provide the maven home path when using Gin.");
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
     */
    public static File findMavenHomeFile() {
        return FileUtils.getFile(findMavenHomePath());
    }

}
