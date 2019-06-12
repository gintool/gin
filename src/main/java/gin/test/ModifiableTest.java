package gin.test;

import java.lang.annotation.Annotation;

/**
 * Used for setting timeout through changing org.junit.Test annotation at runtime
 */
public class ModifiableTest implements org.junit.Test {

        private long timeout;
        Class<? extends Throwable> expected;
        
        public ModifiableTest(long newTimeout, org.junit.Test clone) {
            this.timeout = newTimeout;
            this.expected = clone.expected();
        }
        
        @Override
        public Class<? extends Annotation> annotationType() {
            return org.junit.Test.class;
        }
        
        @Override
        public long timeout() {
            return timeout;
        }
        
        @Override
        public Class<? extends Throwable> expected() {
            return expected;
        }
        
        @Override
        public String toString() {
            return "@org.junit.UnitTest(timeout=" + timeout() + ", expected=" + expected() + ")";
        }
}
