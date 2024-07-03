package gin.edit.line;

import gin.SourceFile;
import gin.SourceFileLine;
import gin.edit.Edit;

import java.io.Serial;
import java.util.List;
import java.util.Random;

public class SwapLine extends LineEdit {

    @Serial
    private static final long serialVersionUID = -5440774476269461819L;
    public String sourceFile;
    public int sourceLine;
    public String destinationFile;
    public int destinationLine;

    /**
     * create a random SwapLine for the given SourceFile, using the provided RNG
     *
     * @param sourceFile to create an edit for
     * @param rng        random number generator, used to choose the target line
     */
    public SwapLine(SourceFile sourceFile, Random rng) {
        SourceFileLine sf = (SourceFileLine) sourceFile;
        List<Integer> targetMethodLines = sf.getLineIDsNonEmptyOrComments(true);

        this.sourceFile = sourceFile.getRelativePathToWorkingDir();
        this.sourceLine = targetMethodLines.get(rng.nextInt(targetMethodLines.size()));
        this.destinationFile = sourceFile.getRelativePathToWorkingDir();
        this.destinationLine = targetMethodLines.get(rng.nextInt(targetMethodLines.size()));
    }

    public SwapLine(String sourceFile, int sourceLine, String destinationFile, int destinationLine) {
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.destinationFile = destinationFile;
        this.destinationLine = destinationLine;
    }

    public static Edit fromString(String description) {
        String[] tokens = description.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        String source = tokens[1];
        String destination = tokens[3];
        String[] sourceTokens = source.split(":");
        String sourceFile = sourceTokens[0].replace("\"", "");
        int sourceLine = Integer.parseInt(sourceTokens[1]);
        String[] destTokens = destination.split(":");
        String destFile = destTokens[0].replace("\"", "");
        int destLine = Integer.parseInt(destTokens[1]);
        return new SwapLine(sourceFile, sourceLine, destFile, destLine);
    }

    @Override
    public SourceFile apply(SourceFile sourceFile, Object metadata) {
        if (sourceLine == destinationLine) {
            return sourceFile; // no-op
        }

        SourceFileLine sf = (SourceFileLine) sourceFile;
        String line1 = sf.getLine(sourceLine);
        String line2 = sf.getLine(destinationLine);
        if ((line1 != null) && (line2 != null)) { // source and destination not already deleted
            sf = sf.removeLine(sourceLine);
            sf = sf.insertLine(sourceLine, line2);
            sf = sf.removeLine(destinationLine);
            sf = sf.insertLine(destinationLine, line1);
        }

        return sf;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " \"" + sourceFile + "\":" + sourceLine + " <-> \"" +
                destinationFile + "\":" + destinationLine;
    }

}
