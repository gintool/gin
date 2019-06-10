package gin.test;

import java.lang.Throwable;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.notification.Failure;

/**
 * Represents a result of repNumber run of UnitTest
 */
public class UnitTestResult {

    private UnitTest test;
    private int repNumber;

    private boolean passed = false;
    private boolean timedOut = false;
    private String exceptionType = "N/A";
    private String exceptionMessage = "N/A";
    private String expectedValue = "N/A";
    private String actualValue = "N/A";

    private long executionTime = 0;
    private long cpuTime = 0;

    
    public UnitTestResult(UnitTest test, int rep) {
        this.test = test;
        this.repNumber = rep;
    }

    /*============== getters  ==============*/

    public UnitTest getTest() {
        return test;
    }

    public int getRepNumber() { 
        return repNumber; 
    }

    public boolean getPassed() {
        return passed;
    }

    public boolean getTimedOut() {
        return timedOut;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getAssertionExpectedValue() {
        return expectedValue;
    }

    public String getAssertionActualValue() {
        return actualValue;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public long getCPUTime() {
        return cpuTime;
    }

    /*============== setters  ==============*/

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public void setExecutionTime(long testExecutionTime)  {
        this.executionTime = testExecutionTime;
    }

    public void setCPUTime(long testCPUTime)  {
        this.cpuTime = testCPUTime;
    }

    /*============== process failure  ==============*/

    public void addFailure(Failure f)  {

        this.passed = false;

        // only display the root cause for output brevity
        Throwable rootCause = f.getException();
        while(rootCause.getCause() != null &&  rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        this.exceptionType = rootCause.getClass().getName();
        this.exceptionMessage = rootCause.getMessage();
    
        if (this.exceptionType == "org.junit.runners.model.TestTimedOutException") {
            this.timedOut = true;

        } else if (this.exceptionType == "java.lang.AssertionError")  {

                // based on messages thrown: https://github.com/junit-team/junit4/blob/master/src/main/java/org/junit/Assert.java
                String s = this.exceptionMessage;

                // 'expected:<EXPECTED> but was:<ACTUAL>'
                if ( s.contains("expected:<") && s.contains(" but was:<") ) {
                    s = s.substring(s.lastIndexOf("expected:<")+10);
                    s = s.substring(0, s.indexOf(">"));
                    this.expectedValue = s;
                    s = this.exceptionMessage;
                    s = s.substring(s.lastIndexOf(" but was:<")+10);
                    s = s.substring(0, s.indexOf(">"));
                    this.actualValue = s;
                }
                // 'expected: EXPECTED but was: ACTUAL'
                else if ( s.contains("expected: ") && s.contains(" but was: ") ) {
                    s = s.substring(s.lastIndexOf("expected: ")+10);
                    s = s.substring(0, s.indexOf(" but was: "));
                    this.expectedValue = s;
                    s = this.exceptionMessage;
                    s = s.substring(s.lastIndexOf(" but was: ")+10);
                    this.actualValue = s;
                }
                // 'expected same:<EXPECTED> was not:<ACTUAL>
                else if ( s.contains("expected same:<") && s.contains(" was not:<") ) {
                    s = s.substring(s.lastIndexOf("expected same:<")+15);
                    s = s.substring(0, s.indexOf(">"));
                    this.expectedValue = s;
                    s = this.exceptionMessage;
                    s = s.substring(s.lastIndexOf(" was not:<")+10);
                    s = s.substring(0, s.indexOf(">"));
                    this.actualValue = s;
                }
                // 'expected null, but was:<ACTUAL>'
                else if (s.contains("expected null, but was:<")) {
                    this.expectedValue = "null";
                    this.actualValue = s.substring(s.lastIndexOf("expected null, but was:<")+24);
                }
                // Actual: ACTUAL 
                else if (s.contains("Actual: ")) {
                    this.actualValue = s.substring(s.lastIndexOf("Actual: ")+8);
                }
                // 'expected not same' - not processed
        } else if (this.exceptionType == "junit.framework.ComparisonFailure")  {

                this.expectedValue = ((junit.framework.ComparisonFailure)rootCause).getExpected();
                this.actualValue = ((junit.framework.ComparisonFailure)rootCause).getActual();
        }


    } 

    public static UnitTestResult fromString(String testResult, long timeoutMS) throws ParseException {

            UnitTestResult result = null;

            String testName = StringUtils.substringBetween(testResult, "UnitTestResult ", ". Rep");
            UnitTest test = UnitTest.fromString(testName);
            test.setTimeoutMS(timeoutMS);

            try {

                String value = StringUtils.substringBetween(testResult, "Rep number: ", ";");
                int rep = Integer.parseInt(value);

                result = new UnitTestResult(test, rep); 

                value = StringUtils.substringBetween(testResult, "Passed: ",";");
                if (! ( (value.equals("true")) || (value.equals("false")) ) ) {
                    throw new NumberFormatException("true or false expected instead of: " + value);
                }
                result.setPassed(Boolean.parseBoolean(value));

                value = StringUtils.substringBetween(testResult, "Timed out: ", ";");
                if (! ( (value.equals("true")) || (value.equals("false")) ) ) {
                    throw new NumberFormatException("true or false expected instead of: " + value);
                }
                result.setTimedOut(Boolean.parseBoolean(value));

                value = StringUtils.substringBetween(testResult, "Exception Type: ", ";");
                result.setExceptionType(value);
                value = StringUtils.substringBetween(testResult, "Exception Message: ", ";");
                result.setExceptionMessage(value);
                value = StringUtils.substringBetween(testResult, "Assertion Expected: ", ";");
                result.setExpectedValue(value);
                value = StringUtils.substringBetween(testResult, "Assertion Actual: ", ";");
                result.setActualValue(value);

                value = StringUtils.substringBetween(testResult, "Execution Time: ", ";");
                result.setExecutionTime(Long.parseLong(value));
                value = StringUtils.substringBetween(testResult, "CPU Time: ",";");
                result.setCPUTime(Long.parseLong(value));

            } catch (NumberFormatException e) {
                throw new ParseException(e.getMessage(), 0);
            }

            return result;
    }
    
    @Override
    public String toString() {
        return String.format(
                "UnitTestResult %s. " + "Rep number: %d; " +
                        "Passed: %b; Timed out: %b; Exception Type: %s; Exception Message: %s; " +
                        "Assertion Expected: %s; Assertion Actual: %s; Execution Time: %d; CPU Time: %d;",
                test.toString(),
                repNumber,
                passed,
                timedOut,
                exceptionType,
                exceptionMessage,
                expectedValue,
                actualValue,
                executionTime,
        cpuTime);
    }

}
