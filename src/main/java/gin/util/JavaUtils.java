package gin.util;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;

public class JavaUtils {
    // https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    /**
     * report working directories - good for debugging
     */
    public static void logWorkingDirectoryData(String tag) {
        String userDir = System.getProperty("user.dir");
        String basedir = System.getProperty("basedir");
        String mmpd    = System.getProperty("maven.multiModuleProjectDirectory");

        Logger.debug("[" + tag + "] user.dir=" + userDir);
        Logger.debug("[" + tag + "] basedir=" + basedir);
        Logger.debug("[" + tag + "] maven.multiModuleProjectDirectory=" + mmpd);

        // What Java thinks “.” is:
        Logger.debug("[" + tag + "] File('.').abs=" + new File(".").getAbsolutePath());
        Logger.debug("[" + tag + "] Paths.get(\"\").abs=" + Paths.get("").toAbsolutePath());

        // Does the module's test-resources dir exist from here?
        File res = new File("src/test/resources");
        Logger.debug("[" + tag + "] src/test/resources exists? " + res.exists());
    }

    /** Normalize a full classpath string and dedupe entries, preserving order. */
    public static String normalizeAndDedupeClasspath(String cp) {
        LinkedHashSet<String> kept = new LinkedHashSet<>();
        for (String raw : cp.split(java.io.File.pathSeparator)) {
            if (raw == null || raw.isBlank()) continue;
            Path p = Paths.get(raw.trim());
            if (!p.isAbsolute()) {
                p = p.toAbsolutePath();   // <-- make absolute relative to *current* (Gin) CWD
            }
            kept.add(p.normalize().toString());
        }
        return String.join(File.pathSeparator, kept);
    }

    public static String getGinLocation() {
        try {
            URL loc = JavaUtils.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc == null) return "";
            Path p = Paths.get(loc.toURI());
            return p.toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            return "";
        }
    }
}
