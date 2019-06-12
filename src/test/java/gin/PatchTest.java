package gin;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import gin.edit.Edit;
import gin.edit.Edit.EditType;
import gin.edit.line.CopyLine;
import gin.edit.line.DeleteLine;
import gin.edit.line.MoveLine;
import gin.edit.line.ReplaceLine;
import gin.edit.line.SwapLine;
import gin.edit.matched.MatchedCopyStatement;
import gin.edit.matched.MatchedDeleteStatement;
import gin.edit.matched.MatchedReplaceStatement;
import gin.edit.matched.MatchedSwapStatement;
import gin.edit.modifynode.BinaryOperatorReplacement;
import gin.edit.modifynode.ReorderLogicalExpression;
import gin.edit.modifynode.UnaryOperatorReplacement;
import gin.edit.statement.CopyStatement;
import gin.edit.statement.DeleteStatement;
import gin.edit.statement.MoveStatement;
import gin.edit.statement.ReplaceStatement;
import gin.edit.statement.SwapStatement;

public class PatchTest {

    private final static String verySmallExampleSourceFilename = TestConfiguration.EXAMPLE_DIR_NAME + "Small.java";
    private final static String tmpPatchedFilenameTree = verySmallExampleSourceFilename + ".patchedTree";
    private final static String tmpPatchedFilenameLine = verySmallExampleSourceFilename + ".patchedLine";
    private final static Charset charSet = Charset.forName("UTF-8");

    private final static List<EditType> allowableEditTypesTree = Arrays.asList(EditType.STATEMENT, EditType.MODIFY_STATEMENT);

    private SourceFileLine sourceFileLine;
    private SourceFileTree sourceFileTree;
    private Patch patchLine;
    private Patch patchTree;

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
        for (int counter=0; counter < patchLine.size(); counter++) {
            assertEquals(patchLine.edits.get(counter), clonedPatch.edits.get(counter));
        }
        assertEquals(patchLine.sourceFile, clonedPatch.sourceFile);
        assertEquals(patchLine.toString(), clonedPatch.toString());
    }

    @Test
    public void testCloneTree() throws Exception {
        Patch clonedPatch = patchTree.clone();
        assertEquals(patchTree.size(), clonedPatch.size());
        for (int counter=0; counter < patchTree.size(); counter++) {
            assertEquals(patchTree.edits.get(counter), clonedPatch.edits.get(counter));
        }
        assertEquals(patchTree.sourceFile, clonedPatch.sourceFile);
        assertEquals(patchTree.toString(), clonedPatch.toString());
    }

    @Test
    public void addLine() throws Exception {
        Edit edit = new CopyLine(sourceFileLine.getFilename(), 1, sourceFileLine.getFilename(), 3);
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
        SourceFileTree sourceFile = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
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

        // Delete a single statement
        int deleteID = sourceFile.getIDForStatementNumber(2);
        DeleteStatement delete = new DeleteStatement(verySmallExampleSourceFilename, deleteID);
        deletePatch.add(delete);
        String modifiedSource = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource);

        // Move a single statement
        int sourceID = sourceFile.getIDForStatementNumber(1); //a=1
        int blockID = sourceFile.getIDForBlockNumber(0);
        int targetID = sourceFile.getIDForStatementNumber(2); //b=2
       
        MoveStatement moveStatement = new MoveStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, targetID);
        Patch movePatch = new Patch(sourceFile);
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
        int s1 = sourceFile.getIDForStatementNumber(1);
        int s2 = sourceFile.getIDForStatementNumber(2);
        SwapStatement swapStatement = new SwapStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s2);
        Patch swapPatch = new Patch(sourceFile);
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
        int s3 = sourceFile.getIDForStatementNumber(1);
        int s4 = sourceFile.getIDForStatementNumber(4);
        SwapStatement swapStatement2 = new SwapStatement(verySmallExampleSourceFilename, s3, verySmallExampleSourceFilename, s4);
        Patch swapPatch2 = new Patch(sourceFile);
        swapPatch2.add(swapStatement2);
        String swapExpected2 = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        if ((a < b) || (a > c)) {\n" + 
                "            c++;\n" + 
                "        }" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "        int a = 1;\n" +
                "    }\n" +
                "}";
        String swappedSource2 = swapPatch2.apply();
        assertEqualsWithoutWhitespace(swapExpected2, swappedSource2);
        
        // Replace a statement with another
        s1 = sourceFile.getIDForStatementNumber(1);
        s2 = sourceFile.getIDForStatementNumber(2);
        ReplaceStatement replaceStatement = new ReplaceStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s2);
        Patch replacePatch = new Patch(sourceFile);
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
        sourceID = sourceFile.getIDForStatementNumber(2);
        blockID = sourceFile.getIDForStatementNumber(0);
        CopyStatement copyStatement = new CopyStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, sourceID); // copy to just after source statement
        Patch copyPatch = new Patch(sourceFile);
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
        sourceID = sourceFile.getIDForStatementNumber(2);
        blockID = sourceFile.getIDForStatementNumber(0);
        int targetID2 = sourceFile.getIDForStatementNumber(2);
        MoveStatement move = new MoveStatement(verySmallExampleSourceFilename, sourceID, verySmallExampleSourceFilename, blockID, blockID); // use blockID as insertion point to target start of block
        Patch moveCopyPatch = new Patch(sourceFile);
        moveCopyPatch.add(move);
        sourceID = sourceFile.getIDForStatementNumber(2);
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
        s1 = sourceFile.getIDForStatementNumber(1);
        s2 = sourceFile.getIDForStatementNumber(2);
        s3 = sourceFile.getIDForStatementNumber(3);
        blockID = sourceFile.getIDForBlockNumber(0);
        MoveStatement move2 = new MoveStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, blockID, s2); // move a=1 to after b=2
        CopyStatement copy2 = new CopyStatement(verySmallExampleSourceFilename, s2, verySmallExampleSourceFilename, blockID, 0); // duplicate b=2 at start
        DeleteStatement delete2 = new DeleteStatement(verySmallExampleSourceFilename, s3); // delete c=a+b
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
        s3 = sourceFile.getIDForStatementNumber(3);
        blockID = sourceFile.getIDForBlockNumber(0);
        
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
        Patch patch = new Patch(sourceFile);
        patch.add(moveStatementNoop);
        
        // move should do nothing
        String expected = sourceFile.toString();
        String actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);
        
        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);
         
        // swap ====
        SwapStatement swapStatementNoop = new SwapStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s1);
        patch = new Patch(sourceFile);
        patch.add(swapStatementNoop);

        expected = sourceFile.toString();
        actual = patch.apply();
        assertEqualsWithoutWhitespace(expected, actual);

        // delete should still work
        patch.add(delete);
        actual = patch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, actual);
        
        // replace ====
        ReplaceStatement replaceStatementNoop = new ReplaceStatement(verySmallExampleSourceFilename, s1, verySmallExampleSourceFilename, s1);
        patch = new Patch(sourceFile);
        patch.add(replaceStatementNoop);

        expected = sourceFile.toString();
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
        SourceFile sourceFile = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
        
        Random rng = new Random(10);
        
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

        // Delete a single statement
        MatchedDeleteStatement delete = new MatchedDeleteStatement(sourceFile, rng);
        deletePatch.add(delete);
        
        String modifiedSource = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource);

        // applying the same delete again should do nothing
        String modifiedSource2 = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, modifiedSource2);

        // Swap a pair of statements
        rng = new Random(4);
        MatchedSwapStatement swapStatement = new MatchedSwapStatement(sourceFile, rng);
        Patch swapPatch = new Patch(sourceFile);
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
        MatchedReplaceStatement replaceStatement = new MatchedReplaceStatement(sourceFile, rng);
        Patch replacePatch = new Patch(sourceFile);
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
        MatchedCopyStatement copyStatement = new MatchedCopyStatement(sourceFile, rng);
        Patch copyPatch = new Patch(sourceFile);
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
        SourceFile sourceFile = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
        
        // BinaryOperatorReplacement
        Patch borPatch = new Patch(sourceFile);
        Random rng = new Random(10); // produces a < to > switch in the first comparison
        BinaryOperatorReplacement bor = new BinaryOperatorReplacement(sourceFile, rng);
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
        Patch uorPatch = new Patch(sourceFile);
        rng = new Random(10); // produces a < to > switch in the first comparison
        UnaryOperatorReplacement uor = new UnaryOperatorReplacement(sourceFile, rng);
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
        Patch rlePatch = new Patch(sourceFile);
        rng = new Random(1); // swaps a<b with a>c
        ReorderLogicalExpression rle = new ReorderLogicalExpression(sourceFile, rng);
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
        SourceFileTree sourceFile = new SourceFileTree(verySmallExampleSourceFilename, Collections.emptyList());
        Patch patch = new Patch(sourceFile);
        int id = sourceFile.getIDForStatementNumber(1);
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
        Edit edit = new DeleteLine(sourceFile.getFilename(), 4);
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
        assertEquals("| gin.edit.statement.DeleteStatement "+verySmallExampleSourceFilename+":13 | gin.edit.statement.CopyStatement "+verySmallExampleSourceFilename+":1 -> "+verySmallExampleSourceFilename+":3:2 " +
                "| gin.edit.statement.MoveStatement "+verySmallExampleSourceFilename+":3 -> "+verySmallExampleSourceFilename+":4:2 |", patchTree.toString());
    }

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

}

