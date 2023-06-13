package gin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SourceFileTest {

    @Test
    public void testGetRelativePathFromAbsolutePath() {
        String relativePath = Paths.get("examples", "unittests", "Error.java").toString();
        Path sourceFilePath = Paths.get(relativePath).toAbsolutePath();
        SourceFileStub sourceFile = new SourceFileStub(sourceFilePath.toString(), Collections.singletonList("returnTen(int)"));
        assertEquals(relativePath, sourceFile.getRelativePathToWorkingDir());
    }

    @Test
    public void testGetRelativePathFromRelativePath() {
        String relativePath = Paths.get("examples", "unittests", "Error.java").toString();
        SourceFileStub sourceFile = new SourceFileStub(relativePath, Collections.singletonList("returnTen(int)"));
        assertEquals(relativePath, sourceFile.getRelativePathToWorkingDir());
    }

    @Test
    public void testGetRelativePathFromUnnormalizedPath() {
        String relativePath = Paths.get("examples", "unittests", "Error.java").toString();
        Path sourceFilePath = Paths.get(".", ".", relativePath).toAbsolutePath();
        SourceFileStub sourceFile = new SourceFileStub(sourceFilePath.toString(), Collections.singletonList("returnTen(int)"));
        assertEquals(relativePath, sourceFile.getRelativePathToWorkingDir());
    }


    @Test
    public void testGetRelativePathFromSymbolicLink() throws IOException {
        Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);
        String relativePath = Paths.get("examples", "unittests", "Error.java").toString();
        SourceFileStub sourceFile = new SourceFileStub(relativePath, Collections.singletonList("returnTen(int)"));

        Path link =  Paths.get("examples", "symboliclinktounittests");
        FileUtils.forceDeleteOnExit(link.toFile());
        Files.createSymbolicLink(link, Paths.get("examples", "unittests").toAbsolutePath());
        sourceFile.workingDir = link.toString();

        assertEquals("Error.java", sourceFile.getRelativePathToWorkingDir());
    }

    private static class SourceFileStub extends SourceFile {

        public SourceFileStub(String filename, List<String> targetMethodNames) {
            super(filename, targetMethodNames);
        }

        @Override
        public SourceFile copyOf() {
            return null;
        }

        @Override
        public String getSource() {
            return null;
        }
    }
}