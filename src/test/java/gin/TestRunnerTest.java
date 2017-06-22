package gin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

import static org.junit.Assert.*;

public class TestRunnerTest {

    private final static String exampleSourceFilename = "src/test/resources/ExampleTriangleProgram.java";
    private final static String exampleTestFilename = "src/test/resources/ExampleTriangleProgramTest.java";

    TestRunner testRunner;

    @Before
    public void setUp() throws Exception {
        testRunner = new TestRunner(new SourceFile(exampleSourceFilename));
    }

    @Test
    public void testRunner() {
        SourceFile sourceFile = new SourceFile(exampleSourceFilename);
        TestRunner test = new TestRunner(sourceFile);
        assertEquals(sourceFile, test.sourceFile);
    }

    @Test
    public void copySource() throws IOException {

        SourceFile sourceFile = new SourceFile(exampleSourceFilename);
        testRunner.copySource(sourceFile);

        // Should have created a tmp directory
        assertTrue(testRunner.getTmpDir().exists());

        // Should have created temp source file
        File origSource = new File(exampleSourceFilename);
        File tmpSource = new File(testRunner.getTmpDir(), origSource.getName());
        assertTrue(tmpSource.exists());

        // File contents should be same as original
        String expected = FileUtils.readFileToString(origSource, Charset.forName("UTF-8"));
        String actual = FileUtils.readFileToString(tmpSource, Charset.forName("UTF-8"));
        assertEqualsWithoutWhitespace(expected, actual);

        // Test File should be there too
        File origTest = new File(exampleTestFilename);
        File tmpTest = new File(testRunner.getTmpDir(), origTest.getName());
        assertTrue(tmpTest.exists());

        // File contents should be same as original
        String expectedTest = FileUtils.readFileToString(origSource, Charset.forName("UTF-8"));
        String actualTest = FileUtils.readFileToString(tmpSource, Charset.forName("UTF-8"));
        assertEqualsWithoutWhitespace(expectedTest, actualTest);

    }

    @Test
    public void ensureDirectory() throws Exception {

        String tmpDirectory = "94613218163490615254";
        File tmp = new File(tmpDirectory);
        if (tmp.exists()) {
            System.err.println("ERROR: Cannot run unit tests as directory exist: " + tmpDirectory);
            System.exit(-1);
        }

        // Create directory when it doesn't exist
        testRunner.ensureDirectory(tmp);
        assertTrue(tmp.exists());

        // Create it when it does exists
        testRunner.ensureDirectory(tmp);
        assertTrue(tmp.exists());

        // Put a file in it
        File emptyFile = new File(tmp, "empty.txt");
        emptyFile.createNewFile();

        // Create it when it does already exist
        testRunner.ensureDirectory(tmp);
        assertTrue(tmp.exists());
        assertEquals(0, tmp.list().length);

        // Tidy up
        tmp.delete();

    }

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

    @Test
    public void compile() {

        testRunner.ensureDirectory(testRunner.getTmpDir());
        testRunner.copySource(new SourceFile(exampleSourceFilename));
        testRunner.compile();

        // Should have created temp source file
        File origSource = new File(exampleSourceFilename);
        String tmpClassFilename = FilenameUtils.removeExtension(origSource.getName()) + ".class";
        String tmpTestClassFilename = FilenameUtils.removeExtension(origSource.getName()) + "Test.class";

        File tmpClassFile = new File(testRunner.getTmpDir(), tmpClassFilename);
        File tmpTestClassFile = new File(testRunner.getTmpDir(), tmpTestClassFilename);

        assertTrue(tmpClassFile.exists());
        assertTrue(tmpTestClassFile.exists());

    }

}