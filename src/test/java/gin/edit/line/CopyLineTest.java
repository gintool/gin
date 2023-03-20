package gin.edit.line;

import gin.SourceFile;
import gin.SourceFileLine;
import gin.TestConfiguration;
import gin.edit.Edit;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CopyLineTest {

    public static final String TEST_SOURCE = TestConfiguration.EXAMPLE_DIR_NAME + "Small.java";

    SourceFileLine sourceFile;
    CopyLine randomCopyLine;
    CopyLine specificCopyLine;

    @Before
    public void setUp() throws Exception {

        // Create sourcefile instance
        File source = new File(TEST_SOURCE);
        LinkedList<String> methods = new LinkedList<>();
        methods.add("Dummy()");
        this.sourceFile = new SourceFileLine(source.getAbsolutePath(), methods);

        Random rng = new Random(1234);

        randomCopyLine = new CopyLine(sourceFile, rng);
        specificCopyLine = new CopyLine(TEST_SOURCE, 2, TEST_SOURCE, 6);

    }

    @Test
    public void testConstructorRandom() {

        assertEquals(randomCopyLine.sourceFile, this.sourceFile.getRelativePathToWorkingDir());
        assertEquals(randomCopyLine.destinationFile, this.sourceFile.getRelativePathToWorkingDir());

        int linesInFile = 100;
        int methodStart = 4;
        int methodEnd = 6;

        assertTrue(randomCopyLine.sourceLine > 0);
        assertTrue(randomCopyLine.sourceLine <= linesInFile);

        assertTrue(randomCopyLine.destinationLine > 0);
        assertTrue(randomCopyLine.destinationLine >= methodStart);
        assertTrue(randomCopyLine.destinationLine <= methodEnd);

    }

    @Test
    public void testConstructorSpecific() {

        assertEquals(specificCopyLine.sourceFile, TEST_SOURCE);
        assertEquals(specificCopyLine.destinationFile, TEST_SOURCE);

        assertEquals(specificCopyLine.sourceLine, 2);
        assertEquals(specificCopyLine.destinationLine, 6);

    }

    @Test
    public void applyIndexOutOfBounds() {

        specificCopyLine.sourceLine = 100;
        SourceFile result = specificCopyLine.apply(sourceFile);
        assertEquals(sourceFile.toString().trim(), result.toString().trim());

    }

    @Test
    public void applyAlreadyDeleted() {

        sourceFile = sourceFile.removeLine(specificCopyLine.sourceLine);

        SourceFile result = specificCopyLine.apply(sourceFile);
        assertEquals(sourceFile.getSource(), result.getSource());

    }

    @Test
    public void applySuccessfully() throws IOException {

        // Copy line two to six - do it myself, then ask specificCopyLine to do it

        List<String> sourceList = Files.readAllLines(Paths.get(TEST_SOURCE), StandardCharsets.UTF_8);
        sourceList.add(6, sourceList.get(1));
        String[] sourceLines = sourceList.toArray(new String[0]);
        String fullSource = String.join(System.getProperty("line.separator"), sourceLines);

        SourceFile result = specificCopyLine.apply(sourceFile);

        assertEquals(fullSource.trim(), result.getSource().trim());

    }

    @Test
    public void toStringTest() {
        String expected = "gin.edit.line.CopyLine \"" + TEST_SOURCE + "\":2 -> \"" + TEST_SOURCE + "\":6";
        String copyLineDescription = this.specificCopyLine.toString();
        assertEquals(expected, copyLineDescription);
    }

    @Test
    public void getEditType() {
        assertEquals(Edit.EditType.LINE, specificCopyLine.getEditType());
    }


}
