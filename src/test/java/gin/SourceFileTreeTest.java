package gin;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.TryStmt;

public class SourceFileTreeTest {

    private SourceFileTree sourceFile;
    private SourceFileTree sourceFileWithMethod;
    private SourceFileTree sourceFileSmall;
    private final static String exampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String exampleMethodName = "delay()";
    private final static String verySmallExampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Small.java";
    private final static Charset charSet = Charset.forName("UTF-8");

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
        assertEquals(exampleSourceFilename, sourceFile.getFilename());
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
        List<Integer> expected = Arrays.asList(new Integer[] {168, 169, 170, 171, 182});
        assertEquals(expected, sourceFileWithMethod.getStatementIDsInTargetMethod());
    }
    
    @Test
    public void statementNumberFromID() throws Exception {
        assertEquals(0, sourceFileSmall.getStatementNumberForNodeID(7)); // whole class block statement
        assertEquals(1, sourceFileSmall.getStatementNumberForNodeID(8)); // a=1
        assertEquals(2, sourceFileSmall.getStatementNumberForNodeID(14)); // b=2
        assertEquals(3, sourceFileSmall.getStatementNumberForNodeID(20)); // c=a+b
    }
    
    @Test
    public void blockNumberFromID() throws Exception {
        assertEquals(0, sourceFileSmall.getBlockNumberForNodeID(7)); // whole class block statement
        assertEquals(1, sourceFileSmall.getBlockNumberForNodeID(44)); // if... block statement
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
    expected.put(7, Arrays.asList(new Integer[] {7, 44}));
    expected.put(8, Arrays.asList(new Integer[] {8, 14, 20, 45}));
    expected.put(14, Arrays.asList(new Integer[] {8, 14, 20, 45}));
    expected.put(20, Arrays.asList(new Integer[] {8, 14, 20, 45}));
    expected.put(30, Arrays.asList(new Integer[] {30}));
    expected.put(44, Arrays.asList(new Integer[] {7, 44}));
    expected.put(45, Arrays.asList(new Integer[] {8, 14, 20, 45}));
    
        assertEquals(expected, sourceFileSmall.getMatchedStatementLists(true, true));
        assertEquals(expected, sourceFileSmall.getMatchedStatementLists(false, true));
    }
    
    @Test
    public void findNodesByClass() throws Exception {
        List<Integer> expected;
        expected = Arrays.asList(new Integer[] {168, 170, 182});
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(true, BlockStmt.class));
        expected = Arrays.asList(new Integer[] {34, 44, 70, 96, 125, 141, 157, 161, 168, 170, 182});
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(false, BlockStmt.class));
        expected = Collections.emptyList();
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(true, IfStmt.class));
        expected = Arrays.asList(new Integer[] {38, 64, 90, 116, 129, 145});
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(false, IfStmt.class));
        expected = Arrays.asList(new Integer[] {169});
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(true, TryStmt.class));
        expected = Arrays.asList(new Integer[] {169});
        assertEquals(expected, sourceFileWithMethod.getNodeIDsByClass(false, TryStmt.class));
    }

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

}
