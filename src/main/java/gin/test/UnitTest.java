package gin.test;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.text.ParseException;

/**
 * Represents a test that needs to be run by Gin.
 */
public class UnitTest implements Comparable<UnitTest>, Serializable {

    @Serial
    private static final long serialVersionUID = -3894012530058369753L;

    public static long defaultTimeoutMS = 10000L;

    private final String className;
    private final String methodName;
    private String moduleName = "";
    private long timeoutMS;

    public UnitTest(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
        this.timeoutMS = defaultTimeoutMS;
    }

    public UnitTest(String className, String methodName, String moduleName) {
        this(className, methodName);
        this.moduleName = moduleName;
    }

    public static UnitTest fromString(String test) throws ParseException {
        if (test == null) throw new ParseException("Null test spec", 0);

        String spec = org.apache.commons.lang3.StringUtils.trim(test);

        // Parse:  "<class>.<method> [<module>]"  (module part optional)
        // Use last " [" to avoid accidental splits if class names ever contain spaces.
        String classAndMethod;
        String moduleName = "";

        int lb = spec.lastIndexOf(" [");
        if (lb > 0 && spec.endsWith("]")) {
            classAndMethod = org.apache.commons.lang3.StringUtils.trim(spec.substring(0, lb));
            moduleName = org.apache.commons.lang3.StringUtils.strip(spec.substring(lb + 1), "[]");
        } else {
            classAndMethod = spec;
        }

        if (!classAndMethod.contains(".")) {
            throw new ParseException("Invalid test selector (expected Class#method or Class.method): " + spec, 0);
        }

        // Accept both "Class#method" and "Class.method"
        String className;
        String methodName;
        int hash = classAndMethod.lastIndexOf('#');
        if (hash >= 0) {
            className = classAndMethod.substring(0, hash).trim();
            methodName = classAndMethod.substring(hash + 1).trim();
        } else {
            className = org.apache.commons.lang3.StringUtils.substringBeforeLast(classAndMethod, ".").trim();
            methodName = org.apache.commons.lang3.StringUtils.substringAfterLast(classAndMethod, ".").trim();
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(className) ||
                org.apache.commons.lang3.StringUtils.isBlank(methodName)) {
            throw new ParseException("Invalid test selector parts in: " + spec, 0);
        }

        // If no module declared, we're done.
        if (org.apache.commons.lang3.StringUtils.isBlank(moduleName)) {
            return new UnitTest(className, methodName);
        }

        java.nio.file.Path cwd = java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        boolean inModule =
                (cwd.getFileName() != null && moduleName.equals(cwd.getFileName().toString()));

        boolean hasChild = java.nio.file.Files.isDirectory(cwd.resolve(moduleName));

        // Also try a short upward search (useful when running under a temp module root)
        boolean foundNearby = false;
        java.nio.file.Path p = cwd;
        for (int i = 0; i < 3 && p != null && !foundNearby; i++, p = p.getParent()) {
            if (java.nio.file.Files.isDirectory(p.resolve(moduleName))) {
                foundNearby = true;
                break;
            }
        }

        if (!(inModule || hasChild || foundNearby)) {
            // Do not fail here; the harness may already have set the working dir.
            // Just log a gentle warning
            try {
                org.pmw.tinylog.Logger.warn(
                        "UnitTest {} declared module '{}', but '{}' has no such subdir nearby; proceeding anyway.",
                        spec, moduleName, cwd);
            } catch (Throwable ignored) { /* logging optional */ }
        }

        // Preserve the module name; harness will use the absolute dir it computed.
        return new UnitTest(className, methodName, moduleName);
    }

    public String getTestName() {
        return className + "." + methodName;
    }

    public String getFullClassName() {
        return className;
    }

    public String getTopClassName() {
        return StringUtils.substringBefore(className, "$");
    }

    public String getInnerClassName() {
        return StringUtils.substringAfter(className, "$");
    }

    public String getModuleName() {
        return moduleName;
    }

    public long getTimeoutMS() {
        return timeoutMS;
    }

    public void setTimeoutMS(long timeoutInMS) {
        this.timeoutMS = timeoutInMS;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public int compareTo(UnitTest other) {

        int classCompare = this.className.compareTo(other.className);

        if (classCompare != 0) {
            return classCompare;
        }

        int methodNameCompare = this.methodName.compareTo(other.methodName);

        if (methodNameCompare != 0) {
            return methodNameCompare;
        }

        return this.moduleName.compareTo(other.moduleName);

    }

    @Override
    public boolean equals(Object obj) {

        return ((obj instanceof UnitTest) && (this.toString()).equals(obj.toString()));

    }

    @Override
    public int hashCode() {
        return (this.toString()).hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s.%s [%s]", className, methodName, moduleName);
    }

}
