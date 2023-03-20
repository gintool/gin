package gin;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class SourceFileTreeTest {

    private final static String exampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String exampleMethodName = "delay()";
    private final static String verySmallExampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Small.java";
    private final static Charset charSet = StandardCharsets.UTF_8;
    private SourceFileTree sourceFile;
    private SourceFileTree sourceFileWithMethod;
    private SourceFileTree sourceFileSmall;

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

    @Before
    public void setup() {
        sourceFile = new SourceFileTree(exampleSourceFilename, Collections.emptyList());
        sourceFileWithMethod = new SourceFileTree(exampleSourceFilename, Collections.singletonList(exampleMethodName));
        sourceFileSmall = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
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
    public void getStatementCount() throws Exception {
        int actualStatementCount = sourceFile.getAllStatementIDs().size();
        int expectedStatementCount = 33;
        assertEquals(expectedStatementCount, actualStatementCount);
    }

    @Test
    public void getStatementCountMethod() throws Exception {
        int actualStatementCount = sourceFileWithMethod.getStatementIDsInTargetMethod().size();
        int expectedStatementCount = 5;
        assertEquals(expectedStatementCount, actualStatementCount);

        actualStatementCount = sourceFile.getStatementIDsInTargetMethod().size();
        expectedStatementCount = 33;
        assertEquals(expectedStatementCount, actualStatementCount);
    }

    @Test
    public void getNumberOfBlocks() throws Exception {
        int actualNumberOfBlocks = sourceFile.getAllBlockIDs().size();
        int expectedNumberOfBlocks = 11;
        assertEquals(expectedNumberOfBlocks, actualNumberOfBlocks);
    }

    @Test
    public void getNumberOfBlocksMethod() throws Exception {
        int actualNumberOfBlocks = sourceFileWithMethod.getBlockIDsInTargetMethod().size();
        int expectedNumberOfBlocks = 3;
        assertEquals(expectedNumberOfBlocks, actualNumberOfBlocks);

        actualNumberOfBlocks = sourceFile.getBlockIDsInTargetMethod().size();
        expectedNumberOfBlocks = 11;
        assertEquals(expectedNumberOfBlocks, actualNumberOfBlocks);
    }

    @Test
    public void getNumberOfInsertionPointsInBlock() throws Exception {
        int blockID = sourceFile.getIDForBlockNumber(1);
        int actualNumberOfInsertionPoints = sourceFile.getInsertionPointsInBlock(blockID).size();
        int expectedNUmberOfInsertionPoints = 4; // 3 statements, can go before first, or after each
        assertEquals(expectedNUmberOfInsertionPoints, actualNumberOfInsertionPoints);
    }

    @Test
    public void statementList() throws Exception {
        String expectedStatementList =
                "[0] {\n int a = 1;\n int b = 2;\n int c = a + b; \nif ((a < b) || (a > c)) {\n c++;\n }\n}\n" +
                        "[1] int a = 1;\n" +
                        "[2] int b = 2;\n" +
                        "[3] int c = a + b;\n" +
                        "[4] if ((a < b) || (a > c)) {\n c++;\n }\n" +
                        "[5] {\n c++;\n }\n" +
                        "[6] c++;\n";
        assertEqualsWithoutWhitespace(expectedStatementList, sourceFileSmall.statementList());
    }

    @Test
    public void statementListMethod() throws Exception {
        List<Integer> expected = Arrays.asList(181, 182, 183, 184, 195);
        assertEquals(expected, sourceFileWithMethod.getStatementIDsInTargetMethod());
    }

    @Test
    public void statementNumberFromID() throws Exception {
        assertEquals(0, sourceFileSmall.getStatementNumberForNodeID(10)); // whole class block statement
        assertEquals(1, sourceFileSmall.getStatementNumberForNodeID(11)); // a=1
        assertEquals(2, sourceFileSmall.getStatementNumberForNodeID(17)); // b=2
        assertEquals(3, sourceFileSmall.getStatementNumberForNodeID(23)); // c=a+b
    }

    @Test
    public void blockNumberFromID() throws Exception {
        assertEquals(0, sourceFileSmall.getBlockNumberForNodeID(10)); // whole class block statement
        assertEquals(1, sourceFileSmall.getBlockNumberForNodeID(47)); // if... block statement
    }

    @Test
    public void matchedStatementSpace() throws Exception {
        // sourceFileSmall
        // the statements are:  [ID] statement
        // [7] whole class without class declaration
        // [8] int a = 1;
        // [14] int b = 2;
        // [20] int c = a + b;
        // [30] if ((a < b) || (a > c)) {
        //    int c = a + b;
        // }
        // [44]{
        //    c++;
        // }
        // [45] c++;
        // We expect 8,14,20,45 to be matched with each other, and 7 to match 44, and 30 to match only itself

        Map<Integer, List<Integer>> expected = new HashMap<>();
        expected.put(10, Arrays.asList(10, 47));
        expected.put(11, Arrays.asList(11, 17, 23, 48));
        expected.put(17, Arrays.asList(11, 17, 23, 48));
        expected.put(23, Arrays.asList(11, 17, 23, 48));
        expected.put(33, List.of(33));
        expected.put(47, Arrays.asList(10, 47));
        expected.put(48, Arrays.asList(11, 17, 23, 48));

        assertEquals(expected, sourceFileSmall.getMatchedStatementLists(true, true));
        assertEquals(expected, sourceFileSmall.getMatchedStatementLists(false, true));
    }

    @Test
    public void findNodesByClass() throws Exception {
        List<Integer> expected;
        expected = Arrays.asList(181, 183, 195);
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(true, BlockStmt.class));
        expected = Arrays.asList(45, 55, 81, 107, 136, 152, 168, 172, 181, 183, 195);
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(false, BlockStmt.class));
        expected = Collections.emptyList();
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(true, IfStmt.class));
        expected = Arrays.asList(49, 75, 101, 127, 140, 156);
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(false, IfStmt.class));
        expected = List.of(182);
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(true, TryStmt.class));
        expected = List.of(182);
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(false, TryStmt.class));
    }

}
