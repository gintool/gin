package gin;

import gin.edit.CopyStatement;
import gin.edit.DeleteStatement;
import gin.edit.Edit;
import gin.edit.MoveStatement;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.Source;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Random;

import static org.junit.Assert.*;

public class PatchTest {

    private final static String exampleSourceFilename = "src/test/resources/ExampleTriangleProgram.java";
    private final static String tmpPatchedFilename = exampleSourceFilename + ".patched";
    private final static String verySmallExampleSourceFilename = "src/test/resources/Small.java";
    private final static Charset charSet = Charset.forName("UTF-8");

    private SourceFile sourceFile;
    private Patch patch;

    @Before
    public void setUp() throws Exception {
        sourceFile = new SourceFile(exampleSourceFilename);
        patch = new Patch(sourceFile);
    }

    @Test
    public void testClone() throws Exception {
        Patch clonedPatch = patch.clone();
        assertEquals(patch.size(), clonedPatch.size());
        for (int counter=0; counter < patch.size(); counter++) {
            assertEquals(patch.edits.get(counter), clonedPatch.edits.get(counter));
        }
        assertEquals(patch.sourceFile, clonedPatch.sourceFile);
        assertEquals(patch.toString(), clonedPatch.toString());
    }

    @Test
    public void add() throws Exception {
        Edit edit = new CopyStatement(1, 2, 3);
        patch.add(edit);
        assertEquals(1, patch.size());
        assertEquals(edit, patch.edits.get(0));
    }

    @Test
    public void size() throws Exception {
        assertEquals(0, patch.size());
        patch.addRandomEdit(new Random(1234));
        patch.addRandomEdit(new Random(1234));
        patch.addRandomEdit(new Random(1234));
        patch.addRandomEdit(new Random(1234));
        assertEquals(4, patch.size());
        patch.remove(0);
        patch.remove(0);
        patch.remove(0);
        assertEquals(1, patch.size());
    }

    @Test
    public void remove() throws Exception {
        patch.addRandomEdit(new Random(1234));
        assertEquals(1, patch.size());
        patch.remove(0);
        assertEquals(0, patch.size());
    }

    @Test
    public void apply() throws Exception {
        SourceFile sourceFile = new SourceFile(verySmallExampleSourceFilename);
        Patch deletePatch = new Patch(sourceFile);

        String deletedExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int a = 1;\n" +
                //"        int b = 2;\n" +
                "        int c = a + b;\n" +
                "    }\n" +
                "}";

        // Delete a single line
        DeleteStatement delete = new DeleteStatement(2);
        deletePatch.add(delete);
        SourceFile deletedLineProgram = deletePatch.apply();
        assertEqualsWithoutWhitespace(deletedExpected, deletedLineProgram.toString());

        // Move a single line
        MoveStatement moveLine = new MoveStatement(1, 0, 2);
        Patch movePatch = new Patch(sourceFile);
        movePatch.add(moveLine);
        String moveExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        int c = a + b;\n" +
                "    }\n" +
                "}";
        SourceFile movedFile = movePatch.apply();
        assertEqualsWithoutWhitespace(moveExpected, movedFile.getSource());


        // Move one line, copy another
        MoveStatement move = new MoveStatement(2, 0, 0);
        Patch moveCopyPatch = new Patch(sourceFile);
        moveCopyPatch.add(move);
        CopyStatement copy = new CopyStatement(2, 0, 2);
        moveCopyPatch.add(copy);

        String copyMoveExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "    }\n" +
                "}";
        SourceFile copyMovedFile = moveCopyPatch.apply();
        assertEqualsWithoutWhitespace(copyMoveExpected, copyMovedFile.getSource());

        // Move copy delete
        String moveCopyDeleteExpected = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int b = 2;\n" +
                "        int a = 1;\n" +
                "    }\n" +
                "}";
        MoveStatement move2 = new MoveStatement(1, 0, 2); // move a=1 to before c=a+b
        CopyStatement copy2 = new CopyStatement(2, 0, 0); // duplicate b=2 at start
        DeleteStatement delete2 = new DeleteStatement(3); // delete c=a+b
        Patch moveCopyDeletePatch = new Patch(sourceFile);
        moveCopyDeletePatch.add(move2);
        moveCopyDeletePatch.add(copy2);
        moveCopyDeletePatch.add(delete2);
        SourceFile moveCopyDeleteProgram = moveCopyDeletePatch.apply();
        assertEqualsWithoutWhitespace(moveCopyDeleteExpected, moveCopyDeleteProgram.toString());


    }

    @Test
    public void addRandomEdit() throws Exception {
        patch.addRandomEdit(new Random(1234));
        assertEquals(1, patch.size());
    }

    @Test
    public void writePatchedSourceToFile() throws Exception {

        // Empty patch should result in original file, ignoring whitespace
        patch.writePatchedSourceToFile(tmpPatchedFilename);
        String patchedText = FileUtils.readFileToString(new File(tmpPatchedFilename), charSet);
        String originalText = FileUtils.readFileToString(new File(exampleSourceFilename), charSet);
        assertEqualsWithoutWhitespace(originalText, patchedText);

        // Patch the small example by deleting first line and write to file
        SourceFile sourceFile = new SourceFile(verySmallExampleSourceFilename);
        Patch patch = new Patch(sourceFile);
        Edit edit = new DeleteStatement(1);
        patch.add(edit);
        patch.writePatchedSourceToFile(tmpPatchedFilename);

        // Check first line removed
        String firstLineRemovedText = FileUtils.readFileToString(new File(tmpPatchedFilename), charSet);
        String expectedSource = "package gin;\n" +
                "public class Small {\n" +
                "    public static void Dummy() {\n" +
                "        int b = 2;\n" +
                "        int c = a + b;\n" +
                "    }\n" +
                "}";
        assertEqualsWithoutWhitespace(expectedSource, firstLineRemovedText);

    }

    @Test
    public void testToString() throws Exception {
        DeleteStatement delete = new DeleteStatement(13);
        CopyStatement copy = new CopyStatement(1, 3, 2);
        MoveStatement move = new MoveStatement(3, 4, 2);
        patch.add(delete);
        patch.add(copy);
        patch.add(move);
        assertEquals("| DEL 13 | COPY 1 -> 3:2 | MOVE 3 -> 4:2 |", patch.toString());
    }

    public static void assertEqualsWithoutWhitespace(String s1, String s2) {
        String s1NoWhitespace = s1.replaceAll("\\s+", "");
        String s2NoWhitespace = s2.replaceAll("\\s+", "");
        assertEquals(s1NoWhitespace, s2NoWhitespace);
    }

}