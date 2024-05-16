package gin.test;

import com.sampullara.cli.Args;

import edu.emory.mathcs.backport.java.util.Arrays;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pmw.tinylog.Logger;

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

/**
 * Runs a given test request. Uses sockets to communicate with ExternalTestRunner.
 */
public class TestHarness implements Serializable {

    public static final String PORT_PREFIX = "PORT";
    @Serial
    private static final long serialVersionUID = -6547478455821943382L;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public TestHarness(String[] args) {
        Args.parseOrExit(this, args);
        start();
    }

    public static void main(String[] args) {
        new TestHarness(args);
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
        int rep;
        long timeoutMS;

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

        LauncherDiscoveryRequest request;

        try {
            Class.forName(className);
            request = buildRequest(test);

        } catch (ClassNotFoundException e) {
            Logger.error("Unable to find test class file: " + className);
            Logger.error("Is the class file on provided classpath?");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        } catch (NoSuchMethodException e) {
            Logger.error(e.getMessage());
            Logger.error("Note that parametirised JUnit tetsts are not allowed in Gin.");
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        } catch (NoSuchFieldException | IllegalAccessException e) {
            Logger.error("Exception when instrumenting tests with a timeout: " + e);
            Logger.error(e.getMessage());
            Logger.trace(e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;

        }

        try (LauncherSession session = LauncherFactory.openSession()) {
            Launcher launcher = session.getLauncher();
            TestPlan testPlan = launcher.discover(request);
            launcher.execute(testPlan, new TestRunListener(result));

        } catch (Exception e) {
            Logger.error("Error running junit: " + e);

            result.setExceptionType(e.getClass().getName());
            result.setExceptionMessage(e.getMessage());
            return result;
        }

        return result;

    }

    public LauncherDiscoveryRequest buildRequest(UnitTest test) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
        ClassLoader loader = this.getClass().getClassLoader();

        String testClassname = test.getFullClassName();
        Class<?> clazz = loader.loadClass(testClassname);

        String methodName = test.getMethodName().replace("()", "");
        Method method = getMethod(clazz, methodName);

        return LauncherDiscoveryRequestBuilder.request()
                .selectors(selectMethod(clazz, method.getName()))
                .configurationParameter("junit.jupiter.execution.timeout.test.method.default", test.getTimeoutMS() + " ms")
                .build();
    }

    
    /*
     * Class.getMethod returns only public methods; Class.getDeclaredMethod ignores superclasses
     * This tries to get all methods including superclasses
     */
    public Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
    	try {
    		return clazz.getDeclaredMethod(methodName);
    	} catch (NoSuchMethodException e) {
    		if (clazz.equals(Object.class)) {
    			throw e;
    		} else {
    			return getMethod(clazz.getSuperclass(), methodName);
    		}
    	}
    	
    }
}    
