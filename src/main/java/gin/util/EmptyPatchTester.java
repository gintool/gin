package gin.util;

import gin.SourceFileLine;
import gin.test.UnitTestResultSet;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.Serial;

/**
 * Runs all tests found in the methodFile for a given project through Gin.
 * <p>
 * Required input: projectDirectory, methodFile, projectName (Gradle/Maven)
 * Required input: projectDirectory, methodFile, classPath (otherwise)
 * <p>
 * methodFile will usually be the output file of gin.util.Profiler
 */
public class EmptyPatchTester extends Sampler {

    @Serial
    private static final long serialVersionUID = 833946988361588298L;

    public EmptyPatchTester(String[] args) {
        super(args);
    }

    // Constructor used for testing
    public EmptyPatchTester(File projectDir, File methodFile) {
        super(projectDir, methodFile);
    }

    public static void main(String[] args) {
        EmptyPatchTester sampler = new EmptyPatchTester(args);
        sampler.sampleMethods();
    }

    protected void sampleMethodsHook() {

        writeHeader();

        // Get the first method
        TargetMethod method = super.methodData.get(0);

        // Get method location
        File source = method.getFileSource();
        String className = method.getClassName();

        // Create source file for line edits for the example method
        SourceFileLine sourceFile = new SourceFileLine(source.getPath(), null);

        Logger.info("Running tests on the original code..");

        // Run all project tests (example sourceFile and className needed for TestRunner setup)
        UnitTestResultSet results = testEmptyPatch(className, super.testData, sourceFile);

        writeResults(results);

        Logger.info("Results saved to: " + super.outputFile.getAbsolutePath());
    }

}
