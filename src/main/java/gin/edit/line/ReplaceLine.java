package gin.edit.line;

import java.util.List;
import java.util.Random;

import gin.SourceFile;
import gin.SourceFileLine;
import gin.edit.Edit;

public class ReplaceLine extends LineEdit {

    public String sourceFile;
    public int sourceLine;
    public String destinationFile;
    public int destinationLine;

    /** 
     * create a random ReplaceLine for the given SourceFile, using the provided RNG
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target line
     * */
    public ReplaceLine(SourceFile sourceFile, Random rng) {
        SourceFileLine sf = (SourceFileLine)sourceFile;
        List<Integer> allLines = sf.getAllLineIDs();
        List<Integer> targetMethodLines = sf.getLineIDsInTargetMethod();
        
        this.sourceFile = sourceFile.getFilename();
        this.sourceLine = allLines.get(rng.nextInt(allLines.size()));
        this.destinationFile = sourceFile.getFilename();
        this.destinationLine = targetMethodLines.get(rng.nextInt(targetMethodLines.size()));
    }
    
    public ReplaceLine(String sourceFile, int sourceLine, String destinationFile, int destinationLine) {
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.destinationFile = destinationFile;
        this.destinationLine = destinationLine;
    }
    
    @Override
    public SourceFile apply(SourceFile sourceFile) {
        if (sourceLine == destinationLine) {
            return sourceFile; // no-op
        }
        
        SourceFileLine sf = (SourceFileLine)sourceFile;
        String lineSource = sf.getLine(sourceLine);
        String lineDestination = sf.getLine(sourceLine);
        if ((lineSource != null) && (lineDestination != null)) { // neither source or target is already deleted
            sf = sf.removeLine(destinationLine);
            sf = sf.insertLine(destinationLine, lineSource);
        }
            
        return sf;
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + sourceFile + ":" + sourceLine + " -> "
                + destinationFile + ":" + destinationLine;
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String source = tokens[1];
        String destination = tokens[3];
        String sourceTokens[] = source.split(":");
        String sourceFile = sourceTokens[0];
        int sourceLine = Integer.parseInt(sourceTokens[1]);
        String destTokens[] = destination.split(":");
        String destFile = destTokens[0];
        int destLine = Integer.parseInt(destTokens[1]);
        return new ReplaceLine(sourceFile, sourceLine, destFile, destLine);
    }

}
