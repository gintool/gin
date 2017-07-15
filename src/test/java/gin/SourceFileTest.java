package gin;

import com.github.javaparser.ast.CompilationUnit;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class SourceFileTest {

    private SourceFile sourceFile;
    private final static String exampleSourceFilename = "src/test/resources/ExampleTriangleProgram.java";
    private final static String verySmallExampleSourceFilename = "src/test/resources/Small.java";
    private final static Charset charSet = Charset.forName("UTF-8");

    @Before
    public void setup() {
        sourceFile = new SourceFile(exampleSourceFilename);
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
    public void getCompilationUnit() throws Exception {
        String expectedSource = FileUtils.readFileToString(new File(exampleSourceFilename), charSet);
        CompilationUnit cu = sourceFile.getCompilationUnit();
        assertEqualsWithoutWhitespace(expectedSource, cu.toString());
    }

    @Test
    public void getStatementCount() throws Exception {
        int actualStatementCount = sourceFile.getStatementCount();
        int expectedStatementCount = 33;
        assertEquals(expectedStatementCount, actualStatementCount);
    }

    @Test
    public void getNumberOfBlocks() throws Exception {
        int actualNumberOfBlocks = sourceFile.getNumberOfBlocks();
        int expectedNumberOfBlocks = 11;
        assertEquals(expectedNumberOfBlocks, actualNumberOfBlocks);
    }

    @Test
    public void getNumberOfInsertionPointsInBlock() throws Exception {
        int actualNumberOfInsertionPoints = sourceFile.getNumberOfInsertionPointsInBlock(1);
        int expectedNUmberOfInsertionPoints = 3;
        assertEquals(expectedNUmberOfInsertionPoints, actualNumberOfInsertionPoints);
    }

    @Test
    public void statementList() throws Exception {
        SourceFile small = new SourceFile(verySmallExampleSourceFilename);
        String expectedStatementList =
                "[0] {\n int a = 1;\n int b = 2;\n int c = a + b;\n}\n" +
                "[1] int a = 1;\n" +
                "[2] int b = 2;\n" +
                "[3] int c = a + b;\n";
        assertEqualsWithoutWhitespace(expectedStatementList, small.statementList());
    }

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

}