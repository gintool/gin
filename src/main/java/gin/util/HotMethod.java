package gin.util;

import gin.test.UnitTest;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a profiled method that will be a target for improvement.
 *
 * @author Giovani
 */
public class HotMethod implements Comparable<HotMethod>, Serializable {

    @Serial
    private static final long serialVersionUID = 853623680385176566L;

    /**
     * The fully qualified class name.
     */
    protected String className;

    /**
     * The name of the method.
     */
    protected String methodName;

    /**
     * Number of times this method was executed.
     */
    protected int count;

    /**
     * The set of test cases to be executed against this method in the future.
     */
    protected Set<UnitTest> tests;

    /**
     * Constructs a hot method.
     *
     * @param className  fully qualified name of the class
     * @param methodName name of the method
     * @param count      number of times this method was executed
     * @param tests      set of test cases to execute against it in the future
     */
    public HotMethod(String className, String methodName, int count, Set<UnitTest> tests) {
        this.className = className;
        this.methodName = methodName;
        this.count = count;
        this.tests = tests;
    }

    /**
     * Constructs a hot method.
     *
     * @param className  fully qualified name of the class
     * @param methodName name of the method
     */
    public HotMethod(String className, String methodName) {
        this(className, methodName, 0, new HashSet<>());
    }

    /**
     * Gets the full method name.
     *
     * @return className.methodName
     */
    public String getFullMethodName() {
        return this.className + "." + this.methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Set<UnitTest> getTests() {
        return tests;
    }

    public void setTests(Set<UnitTest> tests) {
        this.tests = tests;
    }

    /**
     * Compares two hot methods based on the number of times the methods were
     * executed.
     *
     * @param otherMethod other method
     * @return the value {@code 0} if
     * {@code this.getCount() == otherMethod.getCount()}; a value less
     * than {@code 0} if
     * {@code this.getCount() < otherMethod.getCount()}; and a value
     * greater than {@code 0} if
     * {@code this.getCount() > otherMethod.getCount()}
     */
    @Override
    public int compareTo(HotMethod otherMethod) {
        return Integer.compare(this.count, otherMethod.count);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.className);
        hash = 97 * hash + Objects.hashCode(this.methodName);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HotMethod other = (HotMethod) obj;
        if (!Objects.equals(this.className, other.className)) {
            return false;
        }
        return Objects.equals(this.methodName, other.methodName);
    }

    @Override
    public String toString() {
        return "HotMethod{" + "className=" + className
                + ", methodName=" + methodName
                + ", count=" + count
                + ", tests=" + tests + '}';
    }

}
