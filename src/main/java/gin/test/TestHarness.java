package gin.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.IllegalAccessException;
import java.lang.NoSuchFieldException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVWriter;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.apache.commons.lang3.StringUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.pmw.tinylog.Logger;

/** 
 * Runs a given test request. Uses sockets to communicate with ExternalTestRunner.
 */
public class TestHarness {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    public static final String PORT_PREFIX = "PORT";

    public static void main(String[] args) {
        TestHarness testHarness= new TestHarness(args);
    }

    public TestHarness(String[] args) {
        Args.parseOrExit(this, args);
        start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            System.out.println(PORT_PREFIX + "=" + port); // tell the ExternalTestRunner what port we'll be using
                    
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String command;
            while ((command = in.readLine()) != null) {
                try {
                    String response = runTest(command);
                    out.println(response);
                } catch (ParseException e) {
                    break; 
                }
            }
        stop();

        } catch (IOException e) {
            Logger.error(e.getMessage());
        }

    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            Logger.error(e.getMessage());
        }

    }
    private String runTest(String command) throws ParseException {

       String testName;
       Integer rep;
       Long timeoutMS;

        String[] params = command.split(",");
        try {
            if (params.length == 3) {
                testName = params[0];
                rep = Integer.parseInt(params[1]);
                timeoutMS = Long.parseLong(params[2]);
            } else {
                throw new ParseException("Not a test format: " + command, 0);
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Not a test format: " + command, 0);
        }

       UnitTest test = UnitTest.fromString(testName);
       test.setTimeoutMS(timeoutMS);
       UnitTestResult result = runTest(test, rep);

       return result.toString();

    }

    private UnitTestResult runTest(UnitTest test, int rep) {

        UnitTestResult result = new UnitTestResult(test, rep);

        String className = test.getFullClassName();
        String methodName = test.getMethodName();
        long timeout = test.getTimeoutMS();

        Class<?> clazz = null;

        try {
            clazz = Class.forName(className);

        } catch (ClassNotFoundException e) {
            Logger.error("Unable to find test class file: " + className);
            Logger.error("Is the class file on provided classpath?");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        try {
            JUnitBridge.annotateTestWithTimeout(clazz, methodName, timeout);

        } catch (NoSuchMethodException e) {
            Logger.error(e.getMessage());
            Logger.error("Note that parametirised JUnit tetsts are not allowed in Gin.");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        } catch (NoSuchFieldException e) {
            Logger.error("Exception when instrumenting tests with a timeout: " + e);
            Logger.error(e.getMessage());
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        
        } catch (IllegalAccessException e) {
            Logger.error("Exception when instrumenting tests with a timeout: " + e);
            Logger.error(e.getMessage());
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        Request request = Request.method(clazz, methodName);

        JUnitCore jUnitCore = new JUnitCore();

        jUnitCore.addListener(new TestRunListener(result));

        try {
            jUnitCore.run(request);

        } catch (Exception e) {
            Logger.error("Error running junit: " + e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        return result;

    }

}    
