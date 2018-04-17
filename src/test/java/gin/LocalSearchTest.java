package gin;

import com.github.javaparser.ast.CompilationUnit;
import gin.edit.DeleteStatement;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Random;

import static org.junit.Assert.*;

public class LocalSearchTest {


    private final static String examplePackageDirectory = "src/test/resources/";
    private final static String exampleClassName = "ATriangle";
    private final static String exampleSourceFilename = examplePackageDirectory + exampleClassName + ".java";

    LocalSearch localSearch;

    @Before
    public void setUp() throws Exception {
        localSearch = new LocalSearch(exampleSourceFilename);
    }

    @Test
    public void localSearch() {
        localSearch = new LocalSearch(exampleSourceFilename);
        assertEquals(exampleSourceFilename, localSearch.sourceFile.getFilename());
        assertEquals(new File(examplePackageDirectory), localSearch.topDirectory);
        assertEquals(exampleClassName, localSearch.className);
        assertNotNull(localSearch.testRunner);
        assertNotNull(localSearch.rng);
    }

    @Test
    public void neighbour() throws Exception {
        SourceFile sourceFile = new SourceFile(exampleSourceFilename);
        Patch patch = new Patch(sourceFile);
        Random rng = new Random(1234);

        // Neighbour of an empty patch has exactly one edit
        Patch neighbourPatch = localSearch.neighbour(patch, rng);
        assertEquals(neighbourPatch.size(), 1);

        // Now do 10 random neighbours
        for (int i=0; i<10; i++) {
            Patch oneEditPatch = new Patch(sourceFile);
            DeleteStatement delete = new DeleteStatement(15);
            oneEditPatch.add(delete);

            Patch anotherNeighbour = localSearch.neighbour(oneEditPatch, rng);

            boolean addedAnEdit = anotherNeighbour.size() == 2;
            boolean removedAnEdit = anotherNeighbour.size() == 0;

            assertTrue(addedAnEdit || removedAnEdit);

        }

    }

}