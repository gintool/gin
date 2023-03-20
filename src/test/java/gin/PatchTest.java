package gin;

import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.edit.insert.*;
import gin.edit.line.*;
import gin.edit.matched.MatchedCopyStatement;
import gin.edit.matched.MatchedDeleteStatement;
import gin.edit.matched.MatchedReplaceStatement;
import gin.edit.matched.MatchedSwapStatement;
import gin.edit.modifynode.BinaryOperatorReplacement;
import gin.edit.modifynode.ReorderLogicalExpression;
import gin.edit.modifynode.UnaryOperatorReplacement;
import gin.edit.statement.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class PatchTest {

    private final static String verySmallExampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Small.java";
    private final static String tmpPatchedFilenameTree = verySmallExampleSourceFilename + ".patchedTree";
    private final static String tmpPatchedFilenameLine = verySmallExampleSourceFilename + ".patchedLine";
    private final static Charset charSet = StandardCharsets.UTF_8;

    private final static List<EditType> allowableEditTypesTree = Arrays.asList(EditType.STATEMENT, EditType.MODIFY_STATEMENT);

    private SourceFileLine sourceFileLine;
    private SourceFileTree sourceFileTree;
    private Patch patchLine;
    private Patch patchTree;

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

    @Before
    public void setUp() throws Exception {
        sourceFileLine = new SourceFileLine(verySmallExampleSourceFilename, Collections.emptyList());
        sourceFileTree = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
        patchLine = new Patch(sourceFileLine);
        patchTree = new Patch(sourceFileTree);
    }

    @Test
    public void testCloneLine() throws Exception {
        Patch clonedPatch = patchLine.clone();
        assertEquals(patchLine.size(), clonedPatch.size());
        for (int counter = 0; counter < patchLine.size(); counter++) {
            assertEquals(patchLine.edits.get(counter), clonedPatch.edits.get(counter));
        }
        assertEquals(patchLine.sourceFile, clonedPatch.sourceFile);
        assertEquals(patchLine.toString(), clonedPatch.toString());
    }

    @Test
    public void testCloneTree() throws Exception {
        Patch clonedPatch = patchTree.clone();
        assertEquals(patchTree.size(), clonedPatch.size());
        for (int counter = 0; counter < patchTree.size(); counter++) {
            assertEquals(patchTree.edits.get(counter), clonedPatch.edits.get(counter));
        }
        assertEquals(patchTree.sourceFile, clonedPatch.sourceFile);
        assertEquals(patchTree.toString(), clonedPatch.toString());
    }

    @Test
    public void addLine() throws Exception {
        Edit edit = new CopyLine(sourceFileLine.getRelativePathToWorkingDir(), 1, sourceFileLine.getRelativePathToWorkingDir(), 3);
        patchLine.add(edit);
        assertEquals(1, patchLine.size());
        assertEquals(edit, patchLine.edits.get(0));
    }

    @Test
    public void addTree() throws Exception {
        Edit edit = new CopyStatement(verySmallExampleSourceFilename, 1, verySmallExampleSourceFilename, 2, 3);
        patchTree.add(edit);
        assertEquals(1, patchTree.size());
        assertEquals(edit, patchTree.edits.get(0));
    }

    @Test
    public void size() throws Exception {
        assertEquals(0, patchTree.size());
        patchTree.addRandomEdit(new Random(1234), allowableEditTypesTree);
        patchTree.addRandomEdit(new Random(1234), allowableEditTypesTree);
        patchTree.addRandomEdit(new Random(1234), allowableEditTypesTree);
        patchTree.addRandomEdit(new Random(1234), allowableEditTypesTree);
        assertEquals(4, patchTree.size());
        patchTree.remove(0);
        patchTree.remove(0);
        patchTree.remove(0);
        assertEquals(1, patchTree.size());
    }

    @Test
    public void remove() throws Exception {
        patchTree.addRandomEdit(new Random(1234), allowableEditTypesTree);
        assertEquals(1, patchTree.size());
        patchTree.remove(0);
        assertEquals(0, patchTree.size());
    }

    @Test
    public void applyStatements() throws Exception {
        Patch deletePatch = new Patch(sourceFileTree);

        String deletedExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                //"        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";

        // Delete a single statement
        int deleteID = this.sourceFileTree.getIDForStatementNumber(2);
        DeleteStatement delete = new DeleteStatement(verySmallExampleSourceFilename, deleteID);
        deletePatch.add(delete);
        String modifiedSource = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource);

        // Move a single statement
        int sourceID = this.sourceFileTree.getIDForStatementNumber(1); //a=1
        int blockID = this.sourceFileTree.getIDForBlockNumber(0);
        int targetID = this.sourceFileTree.getIDForStatementNumber(2); //b=2

        MoveStatement moveStatement = new MoveStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, targetID);
        Patch movePatch = new Patch(this.sourceFileTree);
        movePatch.add(moveStatement);
        String moveExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String movedSource = movePatch.apply();
        assertEqualsWithoutWhitespace(moveExpected, movedSource);

        // Swap a pair of statements, same type
        int s1 = this.sourceFileTree.getIDForStatementNumber(1);
        int s2 = this.sourceFileTree.getIDForStatementNumber(2);
        SwapStatement swapStatement = new SwapStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s2);
        Patch swapPatch = new Patch(this.sourceFileTree);
        swapPatch.add(swapStatement);
        String swapExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String swappedSource = swapPatch.apply();
        assertEqualsWithoutWhitespace(swapExpected, swappedSource);

        // Swap a pair of statements, different type
        int s3 = this.sourceFileTree.getIDForStatementNumber(1);
        int s4 = this.sourceFileTree.getIDForStatementNumber(4);
        SwapStatement swapStatement2 = new SwapStatement(verySmallExampleSourceFilename, s3, verySmallExampleSourceFilename, s4);
        Patch swapPatch2 = new Patch(this.sourceFileTree);
        swapPatch2.add(swapStatement2);
        String swapExpected2 = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        int a = 1;\n" +
                "    }\n" +
                "}";
        String swappedSource2 = swapPatch2.apply();
        assertEqualsWithoutWhitespace(swapExpected2, swappedSource2);

        // Replace a statement with another
        s1 = this.sourceFileTree.getIDForStatementNumber(1);
        s2 = this.sourceFileTree.getIDForStatementNumber(2);
        ReplaceStatement replaceStatement = new ReplaceStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s2);
        Patch replacePatch = new Patch(this.sourceFileTree);
        replacePatch.add(replaceStatement);
        String replaceExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String replacedSource = replacePatch.apply();
        assertEqualsWithoutWhitespace(replaceExpected, replacedSource);


        // Copy a statement
        sourceID = this.sourceFileTree.getIDForStatementNumber(2);
        blockID = this.sourceFileTree.getIDForStatementNumber(0);
        CopyStatement copyStatement = new CopyStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, sourceID); // copy to just after source statement
        Patch copyPatch = new Patch(this.sourceFileTree);
        copyPatch.add(copyStatement);
        String copyExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String copiedSource1 = copyPatch.apply();
        assertEqualsWithoutWhitespace(copyExpected, copiedSource1);

        // Move one statement, copy another
        sourceID = this.sourceFileTree.getIDForStatementNumber(2);
        blockID = this.sourceFileTree.getIDForStatementNumber(0);
        int targetID2 = this.sourceFileTree.getIDForStatementNumber(2);
        MoveStatement move = new MoveStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, blockID); // use blockID as insertion point to target start of block
        Patch moveCopyPatch = new Patch(this.sourceFileTree);
        moveCopyPatch.add(move);
        sourceID = this.sourceFileTree.getIDForStatementNumber(2);
        CopyStatement copy = new CopyStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, targetID2);
        moveCopyPatch.add(copy);

        String copyMoveExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                //  "        int b = 2;\n" +   // this doesn't appear as the original statement was deleted
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String copiedSource = moveCopyPatch.apply();
        assertEqualsWithoutWhitespace(copyMoveExpected, copiedSource);

        // Move copy delete
        String moveCopyDeleteExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        s1 = this.sourceFileTree.getIDForStatementNumber(1);
        s2 = this.sourceFileTree.getIDForStatementNumber(2);
        s3 = this.sourceFileTree.getIDForStatementNumber(3);
        blockID = this.sourceFileTree.getIDForBlockNumber(0);
        MoveStatement move2 = new MoveStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, blockID, s2); // move a=1 to after b=2
        CopyStatement copy2 = new CopyStatement(verySmallExampleSourceFilename, s2, verySmallExampleSourceFilename, blockID, 0); // duplicate b=2 at start
        DeleteStatement delete2 = new DeleteStatement(verySmallExampleSourceFilename, s3); // delete c=a+b
        Patch moveCopyDeletePatch = new Patch(this.sourceFileTree);
        moveCopyDeletePatch.add(move2);
        moveCopyDeletePatch.add(copy2);
        moveCopyDeletePatch.add(delete2);
        String moveCopyDeleteSource = moveCopyDeletePatch.apply();
        assertEqualsWithoutWhitespace(moveCopyDeleteExpected, moveCopyDeleteSource);


        // now a few checks to ensure that move/swap/replace to the same location are just no-ops
        // (copy will just duplicate the line! so that's fine)
        // we will test for equality in the output (sourcefile is the same)
        // and also that we are still able to retrieve the statementID that
        // was affected (i.e. no operation has occurred)
        s3 = this.sourceFileTree.getIDForStatementNumber(3);
        blockID = this.sourceFileTree.getIDForBlockNumber(0);

        // first an edit to delete the statement that we'll use to test whether it's still there
        delete = new DeleteStatement(verySmallExampleSourceFilename, s3); // delete c=a+b
        deletedExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                //"        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";

        // move ====
        MoveStatement moveStatementNoop = new MoveStatement(verySmallExampleSourceFilename, s3, verySmallExampleSourceFilename, blockID, s3);
        Patch patch = new Patch(this.sourceFileTree);
        patch.add(moveStatementNoop);

        // move should do nothing
        String expected = this.sourceFileTree.toString();
        String actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);

        // swap ====
        SwapStatement swapStatementNoop = new SwapStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s1);
        patch = new Patch(this.sourceFileTree);
        patch.add(swapStatementNoop);

        expected = this.sourceFileTree.toString();
        actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);

        // replace ====
        ReplaceStatement replaceStatementNoop = new ReplaceStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s1);
        patch = new Patch(this.sourceFileTree);
        patch.add(replaceStatementNoop);

        expected = this.sourceFileTree.toString();
        actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);


    }

    @Test
    public void applyLines() throws Exception {
        SourceFile sourceFile = new SourceFileLine(verySmallExampleSourceFilename, Collections.emptyList());
        Patch deletePatch = new Patch(sourceFile);

        String deletedExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                //"        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";

        // Delete a single line
        DeleteLine delete = new DeleteLine(verySmallExampleSourceFilename, 5);
        deletePatch.add(delete);
        String modifiedSource = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource);

        // Move a single line
        MoveLine moveLine = new MoveLine(verySmallExampleSourceFilename, 4, verySmallExampleSourceFilename, 5);
        Patch movePatch = new Patch(sourceFile);
        movePatch.add(moveLine);
        String moveExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String movedSource = movePatch.apply();
        assertEqualsWithoutWhitespace(moveExpected, movedSource);

        // Swap a pair of lines
        SwapLine swapLine = new SwapLine(verySmallExampleSourceFilename, 4, verySmallExampleSourceFilename, 5);
        Patch swapPatch = new Patch(sourceFile);
        swapPatch.add(swapLine);
        String swapExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String swappedSource = swapPatch.apply();
        assertEqualsWithoutWhitespace(swapExpected, swappedSource);

        // Replace a line with another
        ReplaceLine replaceLine = new ReplaceLine(verySmallExampleSourceFilename, 4, verySmallExampleSourceFilename, 5);
        Patch replacePatch = new Patch(sourceFile);
        replacePatch.add(replaceLine);
        String replaceExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String replacepedSource = replacePatch.apply();
        assertEqualsWithoutWhitespace(replaceExpected, replacepedSource);


        // Move one line, copy another
        MoveLine move = new MoveLine(verySmallExampleSourceFilename, 4, verySmallExampleSourceFilename, 6);
        Patch moveCopyPatch = new Patch(sourceFile);
        moveCopyPatch.add(move);
        CopyLine copy = new CopyLine(verySmallExampleSourceFilename, 5, verySmallExampleSourceFilename, 6);
        moveCopyPatch.add(copy);

        String copyMoveExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String copiedSource = moveCopyPatch.apply();
        assertEqualsWithoutWhitespace(copyMoveExpected, copiedSource);

        // Move copy delete
        String moveCopyDeleteExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        MoveLine move2 = new MoveLine(verySmallExampleSourceFilename, 4, verySmallExampleSourceFilename, 5); // move a=1 to before c=a+b
        CopyLine copy2 = new CopyLine(verySmallExampleSourceFilename, 4, verySmallExampleSourceFilename, 4); // duplicate a=1 at start - but it's moved so actually, do nothing
        DeleteLine delete2 = new DeleteLine(verySmallExampleSourceFilename, 6); // delete c=a+b
        Patch moveCopyDeletePatch = new Patch(sourceFile);
        moveCopyDeletePatch.add(move2);
        moveCopyDeletePatch.add(copy2);
        moveCopyDeletePatch.add(delete2);
        String moveCopyDeleteSource = moveCopyDeletePatch.apply();
        assertEqualsWithoutWhitespace(moveCopyDeleteExpected, moveCopyDeleteSource);


        // now a few checks to ensure that move/swap/replace to the same location are just no-ops
        // (copy will just duplicate the line! so that's fine)
        // we will test for equality in the output (sourcefile is the same)
        // and also that we are still able to retrieve the statementID that
        // was affected (i.e. no operation has occurred)

        // first an edit to delete the statement that we'll use to test whether it's still there
        delete = new DeleteLine(verySmallExampleSourceFilename, 6); // delete c=a+b
        deletedExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                //"        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";

        // move ====
        MoveLine moveLineNoop = new MoveLine(verySmallExampleSourceFilename, 6, verySmallExampleSourceFilename, 6);
        Patch patch = new Patch(sourceFile);
        patch.add(moveLineNoop);

        // move should do nothing
        String expected = sourceFile.toString();
        String actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);

        // swap ====
        SwapLine swapLineNoop = new SwapLine(verySmallExampleSourceFilename, 6, verySmallExampleSourceFilename, 6);
        patch = new Patch(sourceFile);
        patch.add(swapLineNoop);

        expected = sourceFile.toString();
        actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);

        // replace ====
        ReplaceLine replaceLineNoop = new ReplaceLine(verySmallExampleSourceFilename, 6, verySmallExampleSourceFilename, 6);
        patch = new Patch(sourceFile);
        patch.add(replaceLineNoop);

        expected = sourceFile.toString();
        actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);


    }

    @Test
    public void applyMatchedStatements() throws Exception {
        Random rng = new Random(10);

        Patch deletePatch = new Patch(sourceFileTree);

        String deletedExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                //"        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";

        // Delete a single statement
        MatchedDeleteStatement delete = new MatchedDeleteStatement(sourceFileTree, rng);
        deletePatch.add(delete);

        String modifiedSource = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource);

        // applying the same delete again should do nothing
        String modifiedSource2 = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource2);

        // Swap a pair of statements
        rng = new Random(4);
        MatchedSwapStatement swapStatement = new MatchedSwapStatement(sourceFileTree, rng);
        Patch swapPatch = new Patch(sourceFileTree);
        swapPatch.add(swapStatement);
        String swapExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "            c++;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "        int c = a + b;\n" +
                "        }" +
                "    }\n" +
                "}";
        String swappedSource = swapPatch.apply();
        assertEqualsWithoutWhitespace(swapExpected, swappedSource);

        // Replace a statement with another
        rng = new Random(4);
        MatchedReplaceStatement replaceStatement = new MatchedReplaceStatement(sourceFileTree, rng);
        Patch replacePatch = new Patch(sourceFileTree);
        replacePatch.add(replaceStatement);
        String replaceExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "            c++;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String replacedSource = replacePatch.apply();
        assertEqualsWithoutWhitespace(replaceExpected, replacedSource);

        // Copy statement
        rng = new Random(2);
        MatchedCopyStatement copyStatement = new MatchedCopyStatement(sourceFileTree, rng);
        Patch copyPatch = new Patch(sourceFileTree);
        copyPatch.add(copyStatement);
        String copyExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        {\n" +
                "            c++;\n" +
                "        }" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String copiedSource = copyPatch.apply();
        assertEqualsWithoutWhitespace(copyExpected, copiedSource);


    }

    @Test
    public void applyModifyNodeEdits() throws Exception {
        // BinaryOperatorReplacement
        Patch borPatch = new Patch(sourceFileTree);
        Random rng = new Random(10); // produces a < to > switch in the first comparison
        BinaryOperatorReplacement bor = new BinaryOperatorReplacement(sourceFileTree, rng);
        borPatch.add(bor);
        String borExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a > b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String modifiedSource = borPatch.apply();
        assertEqualsWithoutWhitespace(borExpected, modifiedSource);

        // known issue with UnaryOperatorReplacement and ReorderLogicalExpression
        // JavaParser's node.replace seems to misbehave on expressions
        // UnaryOperatorReplacement
        Patch uorPatch = new Patch(sourceFileTree);
        rng = new Random(10); // produces a < to > switch in the first comparison
        UnaryOperatorReplacement uor = new UnaryOperatorReplacement(sourceFileTree, rng);
        uorPatch.add(uor);
        String uorExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c--;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = uorPatch.apply();
        assertEqualsWithoutWhitespace(uorExpected, modifiedSource);

        // ReorderLogicalExpression
        Patch rlePatch = new Patch(sourceFileTree);
        rng = new Random(1); // swaps a<b with a>c
        ReorderLogicalExpression rle = new ReorderLogicalExpression(sourceFileTree, rng);
        rlePatch.add(rle);
        String rleExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a > c) || (a < b)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = rlePatch.apply();
        assertEqualsWithoutWhitespace(rleExpected, modifiedSource);
    }

    @Test
    public void applyInsertNodeEdits() throws Exception {
        // InsertBreak
        Patch ibPatch = new Patch(sourceFileTree);
        Random rng = new Random(10); // add break as first line of "if" block
        InsertBreak ib = new InsertBreak(sourceFileTree, rng);
        ibPatch.add(ib);
        String ibExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            break;\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        String modifiedSource = ibPatch.apply();
        assertEqualsWithoutWhitespace(ibExpected, modifiedSource);


        // check tostring and fromstring
        assertEquals(InsertBreak.fromString(ib.toString()).toString(), ib.toString());

        // InsertBreakWithIf
        Patch ibfPatch = new Patch(sourceFileTree);
        rng = new Random(10); // add break as first line of "if" block
        InsertBreakWithIf ibf = new InsertBreakWithIf(sourceFileTree, rng);
        ibfPatch.add(ibf);
        String ibfExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            if (a < 0)" +
                "                break;\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = ibfPatch.apply();
        assertEqualsWithoutWhitespace(ibfExpected, modifiedSource);

        // check tostring and fromstring
        assertEquals(InsertBreakWithIf.fromString(ibf.toString()).toString(), ibf.toString());

        // this makes an InsertBreakWithIf without the "if"
        // (no in-scope variables at insert point)
        // and checks we can still fromString it
        rng = new JDKRandomBridge(RandomSource.MT, 65L); // really, java.util.Random won't make a zero on the first nextInt() call, which is needed here
        ibf = new InsertBreakWithIf(sourceFileTree, rng);
        assertEquals(InsertBreakWithIf.fromString(ibf.toString()).toString(), ibf.toString());


        // InsertContinue
        Patch icPatch = new Patch(sourceFileTree);
        rng = new Random(10); // add continue as first line of "if" block
        InsertContinue ic = new InsertContinue(sourceFileTree, rng);
        icPatch.add(ic);
        String icExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            continue;\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = icPatch.apply();
        assertEqualsWithoutWhitespace(icExpected, modifiedSource);


        // check tostring and fromstring
        assertEquals(InsertContinue.fromString(ic.toString()).toString(), ic.toString());


        // InsertContinueWithIf
        Patch icfPatch = new Patch(sourceFileTree);
        rng = new Random(10); // add continue as first line of "if" block
        InsertContinueWithIf icf = new InsertContinueWithIf(sourceFileTree, rng);
        icfPatch.add(icf);
        String icfExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            if (a < 0)" +
                "                          continue;\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = icfPatch.apply();
        assertEqualsWithoutWhitespace(icfExpected, modifiedSource);


        // check tostring and fromstring
        assertEquals(InsertContinueWithIf.fromString(icf.toString()).toString(), icf.toString());


        // this makes an InsertcontinueWithIf without the "if"
        // (no in-scope variables at insert point)
        // and checks we can still fromString it
        rng = new JDKRandomBridge(RandomSource.MT, 65L); // really, java.util.Random won't make a zero on the first nextInt() call, which is needed here
        icf = new InsertContinueWithIf(sourceFileTree, rng);
        assertEquals(InsertContinueWithIf.fromString(icf.toString()).toString(), icf.toString());

        // InsertReturn
        Patch irPatch = new Patch(sourceFileTree);
        rng = new Random(10); // add return as first line of "if" block
        InsertReturn ir = new InsertReturn(sourceFileTree, rng);
        irPatch.add(ir);
        String irExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            return;\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = irPatch.apply();
        assertEqualsWithoutWhitespace(irExpected, modifiedSource);

        // check tostring and fromstring
        assertEquals(InsertReturn.fromString(ir.toString()).toString(), ir.toString());

        // InsertReturn with if condition
        Patch irfPatch = new Patch(sourceFileTree);
        rng = new Random(10); // add return as first line of "if" block
        InsertReturnWithIf irf = new InsertReturnWithIf(sourceFileTree, rng);

        irfPatch.add(irf);
        String irfExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            if (a < 0)\n" +
                "                return;\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        modifiedSource = irfPatch.apply();
        assertEqualsWithoutWhitespace(irfExpected, modifiedSource);

        // check tostring and fromstring
        assertEquals(InsertReturnWithIf.fromString(irf.toString()).toString(), irf.toString());

        // this makes an InsertReturnWithIf without the "if"
        // (no in-scope variables at insert point)
        // and checks we can still fromString it
        rng = new JDKRandomBridge(RandomSource.MT, 65L); // really, java.util.Random won't make a zero on the first nextInt() call, which is needed here
        irf = new InsertReturnWithIf(sourceFileTree, rng);
        assertEquals(InsertReturnWithIf.fromString(irf.toString()).toString(), irf.toString());

    }

    @Test
    public void addRandomEdit() throws Exception {
        patchTree.addRandomEdit(new Random(1234), allowableEditTypesTree);
        assertEquals(1, patchTree.size());
    }

    @Test
    public void writePatchedTreeSourceToFile() throws Exception {

        // Empty patch should result in original file, ignoring whitespace
        patchTree.writePatchedSourceToFile(tmpPatchedFilenameTree);
        File tmpPatchedFile = new File(tmpPatchedFilenameTree);
        String patchedText = FileUtils.readFileToString(tmpPatchedFile, charSet);
        String originalText = FileUtils.readFileToString(new File(verySmallExampleSourceFilename), charSet);
        assertEqualsWithoutWhitespace(originalText, patchedText);

        // Patch the small example by deleting first line and write to file
        Patch patch = new Patch(sourceFileTree);
        int id = sourceFileTree.getIDForStatementNumber(1);
        Edit edit = new DeleteStatement(verySmallExampleSourceFilename, id);
        patch.add(edit);
        patch.writePatchedSourceToFile(tmpPatchedFilenameTree);

        // Check first line removed
        String firstLineRemovedText = FileUtils.readFileToString(new File(tmpPatchedFilenameTree), charSet);
        String expectedSource = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        assertEqualsWithoutWhitespace(expectedSource, firstLineRemovedText);

        Files.deleteIfExists(tmpPatchedFile.toPath());
    }

    @Test
    public void writePatchedLineSourceToFile() throws Exception {

        // Empty patch should result in original file, ignoring whitespace
        patchLine.writePatchedSourceToFile(tmpPatchedFilenameLine);
        File tmpPatchedFile = new File(tmpPatchedFilenameLine);
        String patchedText = FileUtils.readFileToString(tmpPatchedFile, charSet);
        String originalText = FileUtils.readFileToString(new File(verySmallExampleSourceFilename), charSet);
        assertEqualsWithoutWhitespace(originalText, patchedText);

        // Patch the small example by deleting first line and write to file
        SourceFile sourceFile = new SourceFileLine(verySmallExampleSourceFilename, Collections.emptyList());
        Patch patch = new Patch(sourceFile);
        Edit edit = new DeleteLine(sourceFile.getRelativePathToWorkingDir(), 4);
        patch.add(edit);
        patch.writePatchedSourceToFile(tmpPatchedFilenameLine);

        // Check first line removed
        String firstLineRemovedText = FileUtils.readFileToString(new File(tmpPatchedFilenameLine), charSet);
        String expectedSource = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        if ((a < b) || (a > c)) {\n" +
                "            c++;\n" +
                "        }" +
                "    }\n" +
                "}";
        assertEqualsWithoutWhitespace(expectedSource, firstLineRemovedText);

        Files.deleteIfExists(tmpPatchedFile.toPath());

    }

    @Test
    public void testToString() throws Exception {
        DeleteStatement delete = new DeleteStatement(verySmallExampleSourceFilename, 13);
        CopyStatement copy = new CopyStatement(verySmallExampleSourceFilename, 1, verySmallExampleSourceFilename, 3, 2);
        MoveStatement move = new MoveStatement(verySmallExampleSourceFilename, 3, verySmallExampleSourceFilename, 4, 2);
        patchTree.add(delete);
        patchTree.add(copy);
        patchTree.add(move);
        assertEquals("| gin.edit.statement.DeleteStatement \"" + verySmallExampleSourceFilename + "\":13 | gin.edit.statement.CopyStatement \"" + verySmallExampleSourceFilename + "\":1 -> \"" + verySmallExampleSourceFilename + "\":3:2 " +
                "| gin.edit.statement.MoveStatement \"" + verySmallExampleSourceFilename + "\":3 -> \"" + verySmallExampleSourceFilename + "\":4:2 |", patchTree.toString());
    }

}

