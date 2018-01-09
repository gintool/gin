package gin;

import com.sun.xml.internal.rngom.ast.builder.BuildException;
import org.junit.runner.JUnitCore;

import java.util.List;

// see https://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836

public class IsolatedTestRunner {

    public void runTestClasses(List<String> testClasses) throws BuildException {

        // Load classes
        Class<?>[] classes = new Class<?>[testClasses.size()];
        for (int i=0; i<testClasses.size(); i++) {
            String test = testClasses.get(i);
            try {
                classes[i] = Class.forName(test);
            } catch (ClassNotFoundException e) {
                String msg = "Unable to find class file for test ["+test+"]. Make sure all " +
                        "tests sources are either included in this test target via a 'src' " +
                        "declaration.";
                System.err.println(msg);
                System.exit(-1);
            }
        }

        // Run
        JUnitCore junit = new JUnitCore();
        junit.run(classes);
    }

}
