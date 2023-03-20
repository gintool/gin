package gin;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SourceFileLineTest {

    private final static String exampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String exampleMethodName = "delay()";
    private final static String triangleWithCommentsFilename = TestConfiguration.EXAMPLE_DIR_NAME + "TriangleWithComments.java";
    private final static Charset charSet = StandardCharsets.UTF_8;
    private SourceFileLine sourceFile;
    private SourceFileLine sourceFileWithMethod;
    private SourceFileLine sourceFileTriangleWithComments;

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

    @Before
    public void setup() {
        sourceFile = new SourceFileLine(exampleSourceFilename, Collections.emptyList());
        sourceFileWithMethod = new SourceFileLine(exampleSourceFilename, Collections.singletonList(exampleMethodName));
        sourceFileTriangleWithComments = new SourceFileLine(triangleWithCommentsFilename, Collections.singletonList(exampleMethodName));
    }

    @Test
    public void getSource() throws Exception {
        String expectedSource = FileUtils.readFileToString(new File(exampleSourceFilename), charSet);
        assertEqualsWithoutWhitespace(expectedSource, sourceFile.getSource());
    }

    @Test
    public void getFilename() throws Exception {
        assertEquals(exampleSourceFilename, sourceFile.getRelativePathToWorkingDir());
    }

    @Test
    public void getLineCount() throws Exception {
        int actualLineCount = sourceFile.getAllLineIDs().size();
        int expectedLineCount = 51;
        assertEquals(expectedLineCount, actualLineCount);
    }

    @Test
    public void getLineCountMethod() throws Exception {
        int actualLineCount = sourceFileWithMethod.getLineIDsInTargetMethod().size();
        int expectedLineCount = 7;
        assertEquals(expectedLineCount, actualLineCount);
    }

    @Test
    public void getLineIDsEmptyMethod() throws Exception {
        List<Integer> expected = Arrays.asList(2, 7, 16, 20, 21, 23, 30, 36, 44, 55, 57, 62, 65);
        List<Integer> actual = sourceFileTriangleWithComments.getLineIDsEmpty();

        assertEquals(expected, actual);
    }

    @Test
    public void getLineIDsNonEmptyOrCommentsMethod() throws Exception {
        List<Integer> expected = Arrays.asList(1, 3, 4, 5, 6, 15, 22, 25, 26, 27, 28, 29, 31, 32, 33, 34, 35, 37, 38, 39, 40, 41, 42, 43, 45, 46, 48, 49, 50, 51, 52, 53, 54, 56, 58, 59, 60, 61, 63, 64, 66);
        List<Integer> actual = sourceFileTriangleWithComments.getLineIDsNonEmptyOrComments(false);

        assertEquals(expected, actual);

        expected = Arrays.asList(58, 59, 60, 61, 63, 64);
        actual = sourceFileTriangleWithComments.getLineIDsNonEmptyOrComments(true);

        assertEquals(expected, actual);
    }

    @Test
    public void getLineIDsOnlyCommentsMethod() throws Exception {
        List<Integer> expected = Arrays.asList(8, 9, 10, 11, 12, 13, 14, 17, 18, 19, 24, 47);
        List<Integer> actual = sourceFileTriangleWithComments.getLineIDsOnlyComments();

        assertEquals(expected, actual);
    }

}
