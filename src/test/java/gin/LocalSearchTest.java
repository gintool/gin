package gin;

import gin.edit.line.DeleteLine;
import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.*;

public class LocalSearchTest {

    public static final String FILENAME = TestConfiguration.EXAMPLE_DIR_NAME + "Triangle.java";
    private final static String METHOD_NAME = "classifyTriangle(int,int,int)";

    LocalSearch simpleLocalSearch;

    @Before
    public void setUp() {

        Configurator.currentConfig().level(Level.OFF).activate();  // mute logging

        String commandLine = String.format("-f %s -m %s", FILENAME, METHOD_NAME);
        String[] args = commandLine.split(" ");

        simpleLocalSearch = new LocalSearch(args);

    }

    @Test
    public void localSearch() {
        assertEquals(new File(FILENAME).getPath(), simpleLocalSearch.sourceFile.getRelativePathToWorkingDir());
        assertNotNull(simpleLocalSearch.testRunner);
        assertNotNull(simpleLocalSearch.rng);
    }

    @Test
    public void neighbour() {

        SourceFileLine sourceFile = new SourceFileLine(FILENAME, Collections.emptyList());
        Patch patch = new Patch(sourceFile);

        // Neighbour of an empty patch has exactly one edit
        Patch neighbourPatch = simpleLocalSearch.neighbour(patch);
        assertEquals(neighbourPatch.size(), 1);

        // Now do 10 random neighbours
        for (int i = 0; i < 10; i++) {

            Patch oneEditPatch = new Patch(sourceFile);
            DeleteLine delete = new DeleteLine(sourceFile.getRelativePathToWorkingDir(), 15);
            oneEditPatch.add(delete);

            Patch anotherNeighbour = simpleLocalSearch.neighbour(oneEditPatch);

            boolean addedAnEdit = anotherNeighbour.size() == 2;
            boolean removedAnEdit = anotherNeighbour.size() == 0;

            assertTrue(addedAnEdit || removedAnEdit);

        }

    }


}
