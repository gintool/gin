package gin;

import gin.edit.Edit;
import gin.edit.line.*;
import gin.edit.matched.MatchedCopyStatement;
import gin.edit.matched.MatchedDeleteStatement;
import gin.edit.matched.MatchedReplaceStatement;
import gin.edit.matched.MatchedSwapStatement;
import gin.edit.modifynode.BinaryOperatorReplacement;
import gin.edit.modifynode.UnaryOperatorReplacement;
import gin.edit.statement.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class EditTest {

    private final static String exampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String verySmallExampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Small.java";

    private SourceFileTree sourceFileTree;
    private SourceFileTree sourceFileSmallTree;

    @Before
    public void setUp() throws Exception {
        sourceFileTree = new SourceFileTree(exampleSourceFilename, Collections.emptyList());
        sourceFileSmallTree = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
    }

    @Test
    public void testToStringFromStringStatement() throws Exception {
        Edit copy = new CopyStatement(exampleSourceFilename, 1, exampleSourceFilename, 3, 5);
        String s = copy.toString();
        Edit copy2 = CopyStatement.fromString(s);
        String s2 = copy2.toString();

        assertEquals(s, s2);

        Edit delete = new DeleteStatement(exampleSourceFilename, 1);
        s = delete.toString();
        Edit delete2 = DeleteStatement.fromString(s);
        s2 = delete2.toString();

        assertEquals(s, s2);

        Edit move = new MoveStatement(exampleSourceFilename, 1, exampleSourceFilename, 3, 5);
        s = move.toString();
        Edit move2 = MoveStatement.fromString(s);
        s2 = move2.toString();

        assertEquals(s, s2);

        Edit replace = new ReplaceStatement(exampleSourceFilename, 1, exampleSourceFilename, 3);
        s = replace.toString();
        Edit replace2 = ReplaceStatement.fromString(s);
        s2 = replace2.toString();

        assertEquals(s, s2);

        Edit swap = new SwapStatement(exampleSourceFilename, 1, exampleSourceFilename, 3);
        s = swap.toString();
        Edit swap2 = SwapStatement.fromString(s);
        s2 = swap2.toString();

        assertEquals(s, s2);
    }

    @Test
    public void testToStringFromStringLine() throws Exception {
        Edit copy = new CopyLine(exampleSourceFilename, 1, exampleSourceFilename, 3);
        String s = copy.toString();
        Edit copy2 = CopyLine.fromString(s);
        String s2 = copy2.toString();

        assertEquals(s, s2);

        Edit delete = new DeleteLine(exampleSourceFilename, 1);
        s = delete.toString();
        Edit delete2 = DeleteLine.fromString(s);
        s2 = delete2.toString();

        assertEquals(s, s2);

        Edit move = new MoveLine(exampleSourceFilename, 1, exampleSourceFilename, 3);
        s = move.toString();
        Edit move2 = MoveLine.fromString(s);
        s2 = move2.toString();

        assertEquals(s, s2);

        Edit replace = new ReplaceLine(exampleSourceFilename, 1, exampleSourceFilename, 3);
        s = replace.toString();
        Edit replace2 = ReplaceLine.fromString(s);
        s2 = replace2.toString();

        assertEquals(s, s2);

        Edit swap = new SwapLine(exampleSourceFilename, 1, exampleSourceFilename, 3);
        s = swap.toString();
        Edit swap2 = SwapLine.fromString(s);
        s2 = swap2.toString();

        assertEquals(s, s2);
    }

    @Test
    public void testToStringFromStringMatchedStatement() throws Exception {
        Edit copy = new MatchedCopyStatement(exampleSourceFilename, 1, exampleSourceFilename, 3, 5);
        String s = copy.toString();
        Edit copy2 = MatchedCopyStatement.fromString(s);
        String s2 = copy2.toString();

        assertEquals(s, s2);

        Edit delete = new MatchedDeleteStatement(exampleSourceFilename, 1);
        s = delete.toString();
        Edit delete2 = MatchedDeleteStatement.fromString(s);
        s2 = delete2.toString();

        assertEquals(s, s2);

        Random r = new Random(10);
        Edit replace = new MatchedReplaceStatement(sourceFileTree, r);
        s = replace.toString();
        Edit replace2 = MatchedReplaceStatement.fromString(s);
        s2 = replace2.toString();

        assertEquals(s, s2);

        Edit swap = new MatchedSwapStatement(sourceFileTree, r);
        s = swap.toString();
        Edit swap2 = MatchedSwapStatement.fromString(s);
        s2 = swap2.toString();

        assertEquals(s, s2);
    }

    @Test
    public void testToStringFromStringModifyNode() throws Exception {
        Random r = new Random(10);
        Edit bor = new BinaryOperatorReplacement(sourceFileTree, r);
        String s = bor.toString();

        Edit bor2 = BinaryOperatorReplacement.fromString(s);
        String s2 = bor2.toString();

        assertEquals(s, s2);

        Edit uor = new UnaryOperatorReplacement(sourceFileSmallTree, r);
        s = uor.toString();

        Edit uor2 = UnaryOperatorReplacement.fromString(s);
        s2 = uor2.toString();

        assertEquals(s, s2);
    }
}
