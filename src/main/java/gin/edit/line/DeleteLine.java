package gin.edit.line;

import java.util.List;
import java.util.Random;

import gin.SourceFile;
import gin.SourceFileLine;
import gin.edit.Edit;

public class DeleteLine extends LineEdit {

    public String file;
    public int lineToDelete;

    /** 
     * create a random DeleteLine for the given SourceFile, using the provided RNG
     * @param sourceFile to create an edit for
     * @param rng random number generator, used to choose the target line
     * */
    public DeleteLine(SourceFile sourceFile, Random rng) {
        SourceFileLine sf = (SourceFileLine)sourceFile;
        List<Integer> targetMethodLines = sf.getLineIDsInTargetMethod();
        
        this.file = sourceFile.getFilename();
        this.lineToDelete = targetMethodLines.get(rng.nextInt(targetMethodLines.size()));
    }
    
    public DeleteLine(String file, int lineToDelete) {
        this.file = file;
        this.lineToDelete = lineToDelete;
    }


    @Override
    public SourceFile apply(SourceFile sourceFile) {
        SourceFileLine sf = (SourceFileLine)sourceFile;
        return sf.removeLine(lineToDelete);
    }
    

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + " " + file + ":" + lineToDelete;
    }

    public static Edit fromString(String description) {
        String tokens[] = description.split("\\s+");
        String source = tokens[1];
        String sourceTokens[] = source.split(":");
        String sourceFile = sourceTokens[0];
        int sourceLine = Integer.parseInt(sourceTokens[1]);
        return new DeleteLine(sourceFile, sourceLine);
    }

}
