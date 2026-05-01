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

        UnitTest ginTest;

        test = StringUtils.strip(test);

        String[] testSplit = test.split(" ");

        if (testSplit.length == 2) {

            String testName = testSplit[0];
            String moduleName = testSplit[1];

            if ((testName.contains(".")) && (moduleName.startsWith("[")) && (moduleName.endsWith("]"))) {

                String className = StringUtils.substringBeforeLast(testName, ".");
                String methodName = StringUtils.substringAfterLast(testName, ".");

                moduleName = StringUtils.strip(moduleName, "[]");
                if (moduleName.isEmpty()) {
                    ginTest = new UnitTest(className, methodName);
                } else {
                    File moduleDir = new File(moduleName);
                    if ((moduleDir.exists()) && (moduleDir.isDirectory())) {
                        ginTest = new UnitTest(className, methodName, moduleName);
                    } else {
                        throw new ParseException("UnitTest " + test + " not created as module directory " + moduleName + "does not exist.", 0);
                    }
                }

            } else {
                throw new ParseException("UnitTest " + test + " not created due to invalid input format. It should be: <testClassName>.<testMethodName> [<moduleName>]", 0);

            }

        } else {
            throw new ParseException("UnitTest " + test + " not created due to invalid input format. It should be: <testClassName>.<testMethodName> [<moduleName>]", 0);
        }

        return ginTest;
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
